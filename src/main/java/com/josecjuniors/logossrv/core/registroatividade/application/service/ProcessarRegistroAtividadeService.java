package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.progression.application.port.in.ExecuteProgressionUseCase;
import com.josecjuniors.logossrv.core.progression.application.port.out.VersionedProgressionConfigurationResolver;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionApplicationService;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionInputFactory;
import com.josecjuniors.logossrv.core.progression.application.service.ProgressionOutcome;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionConfigurationReference;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionFact;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;


@Service
public class ProcessarRegistroAtividadeService implements ProcessarRegistroAtividadeUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessarRegistroAtividadeService.class);
    private final RegistroAtividadeRepository registroAtividadeRepository;
    private final JogadorRepository jogadorRepository;
    private final ExecuteProgressionUseCase executeProgressionUseCase = new ProgressionApplicationService();
    private final ProgressionProfileMapper progressionProfileMapper = new ProgressionProfileMapper();
    private final VersionedProgressionConfigurationResolver versionedResolver;
    private final ProgressionInputFactory inputFactory;

    @Autowired
    public ProcessarRegistroAtividadeService(RegistroAtividadeRepository registroAtividadeRepository, JogadorRepository jogadorRepository,
                                             VersionedProgressionConfigurationResolver versionedResolver,
                                             ProgressionInputFactory inputFactory) {
        this.registroAtividadeRepository = registroAtividadeRepository;
        this.jogadorRepository = jogadorRepository;
        this.versionedResolver = versionedResolver;
        this.inputFactory = inputFactory;
    }

    public ProcessarRegistroAtividadeService(RegistroAtividadeRepository registroAtividadeRepository, JogadorRepository jogadorRepository) {
        this(registroAtividadeRepository, jogadorRepository, null, null);
    }

    public ProcessarRegistroAtividadeService(RegistroAtividadeRepository registroAtividadeRepository, JogadorRepository jogadorRepository, com.josecjuniors.logossrv.core.common.service.NivelXPService ignoredNivelXPService) {
        this(registroAtividadeRepository, jogadorRepository);
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

        Jogador jogador = registro.getJogador();
        ProgressionProfile currentProfile = progressionProfileMapper.from(jogador);
        var resolved = versionedResolver == null ? java.util.Optional.<com.josecjuniors.logossrv.core.progression.domain.model.ResolvedProgressionConfiguration>empty()
                : versionedResolver.resolveVersioned(new ProgressionConfigurationReference(registro.getAtividadeConfig().getId().getValue()));
        ProgressionInput input = resolved.map(value -> inputFactory.create(
                        new ProgressionFact(registro.getDetalhes().stream()
                                .filter(d -> value.numericFactorKeys().contains(d.getFatorCalculo().getId().getValue().toString()))
                                .map(d -> new ProgressionFact.Detail(d.getFatorCalculo().getId().getValue().toString(), Double.parseDouble(d.getValorRegistrado())))
                                .toList()), value.configuration(), currentProfile))
                .orElseGet(() -> toProgressionInput(registro));
        ProgressionOutcome outcome = executeProgressionUseCase.execute(input, currentProfile);
        ProgressionProfile updatedProfile = outcome.updatedProfile();
        ProgressionResult result = outcome.result();
        progressionProfileMapper.applyTo(jogador, updatedProfile, registro.getAtividadeConfig().getRegrasDistribuicao().stream()
                .map(regra -> regra.getAtributo()).toList());
        jogadorRepository.save(jogador);

        if (resolved.isPresent()) {
            var value = resolved.get();
            registro.marcarComoProcessado((int) result.xpGlobal(), (int) result.stressTotal(),
                    value.configurationVersionId(), value.skillPolicyVersionId());
        } else {
            registro.marcarComoProcessado((int) result.xpGlobal(), (int) result.stressTotal());
        }
        registroAtividadeRepository.save(registro);

        logger.info("Registro de atividade ID: {} processado com sucesso.", event.registroAtividadeId().getValue());
    }

    private ProgressionInput toProgressionInput(RegistroAtividade registro) {
        var detalhes = registro.getDetalhes().stream()
                .filter(detalhe -> detalhe.getFatorCalculo().getTipoInput().ehValorNumerico())
                .map(detalhe -> new ProgressionInput.Detail(
                        detalhe.getFatorCalculo().getId().getValue().toString(),
                        Double.parseDouble(detalhe.getValorRegistrado())))
                .toList();
        var distribuicoes = registro.getAtividadeConfig().getRegrasDistribuicao().stream()
                .map(regra -> new ProgressionInput.AttributeDistribution(
                        regra.getAtributo().getId().getValue().toString(),
                        regra.getPesoPercentual(),
                        regra.getRegraFatorXPS().stream().map(this::toXpRule).toList(),
                        regra.getRegraFatorEstresses().stream().map(this::toStressRule).toList()))
                .toList();
        var bonuses = registro.getJogador().getHabilidades().stream()
                .flatMap(habilidadeJogador -> habilidadeJogador.getHabilidade().getRegrasDistribuicao().stream()
                        .map(regra -> new ProgressionInput.SkillBonus(
                                regra.getAtributo().getId().getValue().toString(),
                                regra.getPesoDistribuicao(),
                                habilidadeJogador.getNivelAtual())))
                .toList();
        return new ProgressionInput(registro.getAtividadeConfig().getXpBase(), registro.getAtividadeConfig().getEstresseBase(), detalhes, distribuicoes, bonuses);
    }

    private ProgressionInput.XpRule toXpRule(RegraFatorXP regra) {
        return new ProgressionInput.XpRule(regra.getFatorCalculo().getId().getValue().toString(), regra.getPesoMultiplicador(), regra.getPontoCorteMin(), regra.getPontoCorteMax());
    }

    private ProgressionInput.StressRule toStressRule(RegraFatorEstresse regra) {
        return new ProgressionInput.StressRule(regra.getPesoMultiplicador(), regra.getPontoCorteMin(), regra.getPontoCorteMax(), regra.getTipo() == TipoFatorEstresse.NEGATIVO ? ProgressionInput.StressType.NEGATIVE : ProgressionInput.StressType.POSITIVE);
    }

}
