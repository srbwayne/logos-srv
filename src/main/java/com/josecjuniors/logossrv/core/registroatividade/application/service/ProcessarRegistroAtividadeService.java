package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.common.service.NivelXPService;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.HabilidadeJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeDetalhe;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaohabilidade.domain.model.RegraDistribuicaoHabilidade;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.ProcessarRegistroAtividadeUseCase;
import com.josecjuniors.logossrv.core.registroatividade.domain.events.RegistroAtividadeCriadoEvent;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.naoNuloEMaiorOuIgualQue;
import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.naoNuloEMenorOuIgualQue;
import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.nuloOuMaiorOuIgualQue;
import static com.josecjuniors.logossrv.core.common.utils.NumberUtils.nuloOuMenorOuIgualQue;

@Service
public class ProcessarRegistroAtividadeService implements ProcessarRegistroAtividadeUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessarRegistroAtividadeService.class);
    private final RegistroAtividadeRepository registroAtividadeRepository;
    private final JogadorRepository jogadorRepository;
    private final NivelXPService nivelXPService;

    public ProcessarRegistroAtividadeService(RegistroAtividadeRepository registroAtividadeRepository, JogadorRepository jogadorRepository, NivelXPService nivelXPService) {
        this.registroAtividadeRepository = registroAtividadeRepository;
        this.jogadorRepository = jogadorRepository;
        this.nivelXPService = nivelXPService;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void processar(RegistroAtividadeCriadoEvent event) {
        processarEvento(event);
    }

    public void processarEvento(RegistroAtividadeCriadoEvent event) {
        logger.info("Processando registro de atividade ID: {}", event.registroAtividadeId().getValue());

        RegistroAtividade registro = registroAtividadeRepository.findByIdWithDetails(event.registroAtividadeId())
                .orElseThrow(() -> new IllegalStateException("Registro de Atividade não encontrado para processamento. ID: " + event.registroAtividadeId().getValue()));

        int xpBase = registro.getAtividadeConfig().getXpBase();
        int estresseBase = registro.getAtividadeConfig().getEstresseBase();

        Map<RegraDistribuicaoAtividade, Double> xpPorRegra = new HashMap<>();
        double estresseAcumulado = estresseBase;

        var detalhesCalculaveis = registro.getDetalhes().stream().filter(detalhe -> detalhe.getFatorCalculo().getTipoInput().ehValorNumerico()).toList();

        for (RegistroAtividadeDetalhe detalhe : detalhesCalculaveis) {
            double valorDetalhe = Double.parseDouble(detalhe.getValorRegistrado());

            for (RegraDistribuicaoAtividade regraDist : registro.getAtividadeConfig().getRegrasDistribuicao()) {
                for (RegraFatorXP regraXP : regraDist.getRegraFatorXPS()) {
                    if (regraXP.getFatorCalculo().equals(detalhe.getFatorCalculo())) {
                        double xpCalculado = calcularXP(regraXP, valorDetalhe, xpBase, regraDist.getPesoPercentual());
                        xpPorRegra.merge(regraDist, xpCalculado, Double::sum);
                    }
                }
                for (RegraFatorEstresse regraEstresse : regraDist.getRegraFatorEstresses()) {
                    estresseAcumulado += calcularEstresse(regraEstresse, valorDetalhe, estresseBase);
                }
            }
        }

        Jogador jogador = registro.getJogador();
        jogador.aplicarEstresse((int) estresseAcumulado);

        long xpTotal = xpPorRegra.isEmpty() ? xpBase : xpPorRegra.values().stream().mapToLong(Double::longValue).sum();

        xpPorRegra.forEach((regra, xp) -> {
            AtributoJogador atributoJogador = jogador.adicionarAtributo(regra.getAtributo());
            double bonusXP = calcularBonusHabilidade(jogador, atributoJogador);
            long xpFinal = xp.longValue() + (long) bonusXP;
            nivelXPService.adicionarExperiencia(atributoJogador, xpFinal);
        });

        nivelXPService.adicionarExperiencia(jogador, xpTotal);
        jogadorRepository.save(jogador);

        registro.marcarComoProcessado((int) xpTotal, (int) estresseAcumulado);
        registroAtividadeRepository.save(registro);

        logger.info("Registro de atividade ID: {} processado com sucesso.", event.registroAtividadeId().getValue());
    }

    private double calcularBonusHabilidade(Jogador jogador, AtributoJogador atributoJogador) {
        double bonusTotal = 0;
        for (HabilidadeJogador habilidadeJogador : jogador.getHabilidades()) {
            for (RegraDistribuicaoHabilidade regra : habilidadeJogador.getHabilidade().getRegrasDistribuicao()) {
                if (regra.getAtributo().equals(atributoJogador.getAtributo())) {
                    // Exemplo de bônus: 5% do peso de distribuição por nível da habilidade
                    bonusTotal += (regra.getPesoDistribuicao() * habilidadeJogador.getNivelAtual() * 0.05);
                }
            }
        }
        return bonusTotal;
    }

    private double calcularXP(RegraFatorXP regraXP, double valor, double xpBase, Double pesoPercentual) {
        if (nuloOuMenorOuIgualQue(regraXP.getPontoCorteMin(), valor) && nuloOuMaiorOuIgualQue(regraXP.getPontoCorteMax(), valor)) {
            return (xpBase * regraXP.getPesoMultiplicador() * pesoPercentual);
        }
        return ((xpBase / regraXP.getPesoMultiplicador()) * pesoPercentual);
    }

    private double calcularEstresse(RegraFatorEstresse regraEstresse, double valor, double estresseBase) {
        double estresseCalculado = estresseBase;
        if (naoNuloEMenorOuIgualQue(regraEstresse.getPontoCorteMin(), valor)) {
            estresseCalculado = estresseBase / regraEstresse.getPesoMultiplicador();
        } else if (naoNuloEMaiorOuIgualQue(regraEstresse.getPontoCorteMax(), valor)) {
            estresseCalculado = estresseBase * regraEstresse.getPesoMultiplicador();
        }
        return regraEstresse.getTipo() == TipoFatorEstresse.NEGATIVO ? -estresseCalculado : estresseCalculado;
    }
}
