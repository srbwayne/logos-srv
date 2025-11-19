package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.AtributoJogadorRepository;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividade;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.model.RegraDistribuicaoAtividadeId;
import com.josecjuniors.logossrv.core.regradistribuicaoatividade.domain.repository.RegraDistribuicaoAtividadeRepository;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.enums.TipoFatorEstresse;
import com.josecjuniors.logossrv.core.regrafatorestresse.domain.repository.RegraFatorEstresseRepository;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXP;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.model.RegraFatorXPId;
import com.josecjuniors.logossrv.core.regrafatorxp.domain.repository.RegraFatorXPRepository;
import com.josecjuniors.logossrv.core.registroatividade.domain.events.RegistroAtividadeCriadoEvent;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividade;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.RegistroAtividadeId;
import com.josecjuniors.logossrv.core.registroatividade.domain.model.enums.StatusProcessamento;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ProcessarRegistroAtividadeServiceTest {

    @Autowired
    private ProcessarRegistroAtividadeService service;
    @Autowired
    private AppUserJpaRepository appUserRepository;
    @Autowired
    private RegistroAtividadeRepository registroAtividadeRepository;
    @Autowired
    private JogadorRepository jogadorRepository;
    @Autowired
    private AtividadeConfigRepository atividadeConfigRepository;
    @Autowired
    private AtributoJogadorRepository atributoJogadorRepository;
    @Autowired
    private AtributoRepository atributoRepository;
    @Autowired
    private RegraDistribuicaoAtividadeRepository regraDistribuicaoRepository;
    @Autowired
    private FatorCalculoRepository fatorCalculoRepository;
    @Autowired
    private RegraFatorXPRepository regraFatorXPRepository;
    @Autowired
    private RegraFatorEstresseRepository regraFatorEstresseRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        registroAtividadeRepository.deleteAll();
        regraFatorXPRepository.deleteAll();
        regraFatorEstresseRepository.deleteAll();
        regraDistribuicaoRepository.deleteAll();
        atividadeConfigRepository.deleteAll();
        jogadorRepository.deleteAll();
        atributoRepository.deleteAll();
        fatorCalculoRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void deveProcessarRegistroECalcularXpEEstresseCorretamente() {
        AppUser testAppUser = new AppUser(new AppUserId(), "perfil.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);
        // Arrange: Cenário de dados rico
        Jogador jogador = new Jogador(JogadorId.generate(), testAppUser, "Tester");
        jogador.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), jogador));

        Atributo forca = atributoRepository.save(new Atributo(new AtributoId(), "Força", ""));
        jogador.adicionarAtributo(forca);
        jogadorRepository.save(jogador);

        AtividadeConfig atividade = new AtividadeConfig(AtividadeConfigId.generate(), "Treino", null, 100, 10, null, null);

        RegraDistribuicaoAtividade regraDist = new RegraDistribuicaoAtividade(new RegraDistribuicaoAtividadeId(), atividade, forca, 1.0);

        FatorCalculo peso = new FatorCalculo(FatorCalculoId.generate(), "Peso", "kg", TipoInput.NUMERICO);
        RegraFatorXP regraFatorXP = new RegraFatorXP(new RegraFatorXPId(), regraDist, peso, 1.5, 80.0, 120.0);

        RegraFatorEstresse regraFatorEstresse = new RegraFatorEstresse(new RegraFatorEstresseId(), regraDist, 2.0, 100.0, null, TipoFatorEstresse.POSITIVO);

        regraDist.adicionarRegraFatorXPS(regraFatorXP);
        regraDist.adicionarRegraFatorEstresses(regraFatorEstresse);

        atividade.adicionarRegraDistribuicao(regraDist);

        atividadeConfigRepository.save(atividade);

        regraFatorXPRepository.save(regraFatorXP);
        regraFatorEstresseRepository.save(regraFatorEstresse);

        RegistroAtividade registro = new RegistroAtividade(RegistroAtividadeId.generate(), jogador, atividade, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        registro.adicionarDetalhe(peso, "100.0"); // Valor que aciona ambas as regras
        registroAtividadeRepository.save(registro);

        // Act
        service.processarEvento(new RegistroAtividadeCriadoEvent(registro.getId()));

        // Assert
        entityManager.flush();
        entityManager.clear();

        RegistroAtividade registroProcessado = registroAtividadeRepository.findById(registro.getId()).get();
        Jogador jogadorAtualizado = jogadorRepository.findById(jogador.getId()).get();
        Set<AtributoJogador> atributosJogador = atributoJogadorRepository.findByJogadorId(jogador.getId());

        // 1. Valida o Registro
        assertThat(registroProcessado.getStatusProcessamento()).isEqualTo(StatusProcessamento.PROCESSADO);
        // XP = 100 (base) * 1.5 (multiplicador) = 150
        assertThat(registroProcessado.getXpGanhoFinal()).isEqualTo(150);
        // Estresse = 10 (base) + (10 / 2.0) = 15
        assertThat(registroProcessado.getEstresseGerado()).isEqualTo(15);

        // 2. Valida o Jogador
        assertThat(jogadorAtualizado.getEstresseGlobal().getPontuacaoAtual()).isEqualTo(15);
        assertThat(jogadorAtualizado.getXpTotal()).isEqualTo(150);
        assertThat(jogadorAtualizado.getNivelAtual()).isEqualTo(2); // 150 XP é o suficiente para o nível 2
        assertThat(jogadorAtualizado.getPontosHabilidade()).isEqualTo(3); // 150 XP é o suficiente para o nível 2

        // 3. Valida o Atributo
        assertThat(atributosJogador.stream().findFirst().get().getXpTotal()).isEqualTo(150);
        assertThat(atributosJogador.stream().findFirst().get().getNivelAtual()).isEqualTo(2);
    }

    @Test
    void deveProcessarRegistroDeAtividadesSemRegraCorretamente() {
        AppUser testAppUser = new AppUser(new AppUserId(), "perfil.test@email.com", passwordEncoder.encode("password"));
        appUserRepository.save(testAppUser);

        Jogador jogador = new Jogador(JogadorId.generate(), testAppUser, "Tester");
        jogador.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), jogador));

        jogadorRepository.save(jogador);

        AtividadeConfig atividade = new AtividadeConfig(AtividadeConfigId.generate(), "Cortar o Cabelo", null, 10, 5, null, null);


        atividadeConfigRepository.save(atividade);

        RegistroAtividade registro = new RegistroAtividade(RegistroAtividadeId.generate(), jogador, atividade, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        registroAtividadeRepository.save(registro);

        // Act
        service.processarEvento(new RegistroAtividadeCriadoEvent(registro.getId()));

        // Assert
        entityManager.flush();
        entityManager.clear();

        RegistroAtividade registroProcessado = registroAtividadeRepository.findById(registro.getId()).get();
        Jogador jogadorAtualizado = jogadorRepository.findById(jogador.getId()).get();

        // 1. Valida o Registro
        assertThat(registroProcessado.getStatusProcessamento()).isEqualTo(StatusProcessamento.PROCESSADO);
        // XP = 10 (base)
        assertThat(registroProcessado.getXpGanhoFinal()).isEqualTo(10);
        // Estresse = 5 (base)
        assertThat(registroProcessado.getEstresseGerado()).isEqualTo(5);

        // 2. Valida o Jogador
        assertThat(jogadorAtualizado.getEstresseGlobal().getPontuacaoAtual()).isEqualTo(5);
        assertThat(jogadorAtualizado.getXpTotal()).isEqualTo(10);
        assertThat(jogadorAtualizado.getNivelAtual()).isEqualTo(1); // nao eh o bastante pra subir de nivel
        assertThat(jogadorAtualizado.getPontosHabilidade()).isEqualTo(1); // nao eh o bastante para ganhar pontos

    }
}
