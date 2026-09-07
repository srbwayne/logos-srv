package com.josecjuniors.logossrv.core.registroatividade.application.service;

import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfig;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;
import com.josecjuniors.logossrv.core.atividadeconfig.domain.repository.AtividadeConfigRepository;
import com.josecjuniors.logossrv.core.atividadeformulario.domain.model.json.TipoInput;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculo;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.model.FatorCalculoId;
import com.josecjuniors.logossrv.core.fatorcalculo.domain.repository.FatorCalculoRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.progression.application.port.out.ActivityProgressionExecutionStore;
import com.josecjuniors.logossrv.core.registroatividade.application.port.in.CreateRegistroAtividadeCommand;
import com.josecjuniors.logossrv.adapters.in.web.registroatividade.dto.request.DetalheRegistroRequest;
import com.josecjuniors.logossrv.core.registroatividade.domain.repository.RegistroAtividadeRepository;
import com.josecjuniors.logossrv.support.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ActivityProgressionTx1AtomicityPostgresIT {
    @Autowired AppUserJpaRepository users;
    @Autowired JogadorRepository jogadores;
    @Autowired AtividadeConfigRepository configs;
    @Autowired FatorCalculoRepository fatores;
    @Autowired CreateRegistroAtividadeService creator;
    @Autowired RegistroAtividadeRepository registros;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @SpyBean ActivityProgressionExecutionStore executionStore;

    @Test
    void sourceAndIntentRollBackTogetherWhenIntentPersistenceFails() {
        var user = users.saveAndFlush(new AppUser(new AppUserId(), "tx1-" + UUID.randomUUID() + "@test",
                encoder.encode("password")));
        jogadores.save(new Jogador(JogadorId.generate(), user, "tx1-player-" + UUID.randomUUID()));
        var config = configs.save(new AtividadeConfig(new AtividadeConfigId(), "tx1-" + UUID.randomUUID(),
                "fixture", 1, 0, null, null));
        var factor = fatores.save(new FatorCalculo(FatorCalculoId.generate(), "tx1-" + UUID.randomUUID(),
                "pages", TipoInput.NUMERICO));

        doThrow(new IllegalStateException("controlled intent failure")).when(executionStore).create(
                any(), anyString(), any(), any(), any(), anyString(), anyInt());

        var command = new CreateRegistroAtividadeCommand(user.getEmail(), config.getId().getValue(),
                LocalDateTime.now().minusHours(1), LocalDateTime.now(),
                List.of(new DetalheRegistroRequest(factor.getId().getValue(), "30")));

        try {
            assertThatThrownBy(() -> creator.create(command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("controlled intent failure");
        } finally {
            reset(executionStore);
        }

        assertThat(registros.findAll()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM progression_external_execution", Long.class)).isZero();
    }
}
