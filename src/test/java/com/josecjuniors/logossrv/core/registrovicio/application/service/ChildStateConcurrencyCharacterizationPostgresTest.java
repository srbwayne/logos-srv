package com.josecjuniors.logossrv.core.registrovicio.application.service;

import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.adapters.out.debuff.jpa.DebuffJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.DebuffJogadorJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.JogadorJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.VicioJogadorJpaRepository;
import com.josecjuniors.logossrv.adapters.out.registrovicio.jpa.RegistroVicioJpaRepository;
import com.josecjuniors.logossrv.adapters.out.vicio.jpa.RegraVicioJpaRepository;
import com.josecjuniors.logossrv.adapters.out.vicio.jpa.VicioJpaRepository;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.debuff.domain.model.Debuff;
import com.josecjuniors.logossrv.core.debuff.domain.model.DebuffId;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogadorId;
import com.josecjuniors.logossrv.core.registrovicio.application.command.CreateRegistroVicioCommand;
import com.josecjuniors.logossrv.core.registrovicio.domain.events.RegistroVicioCriadoEvent;
import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicio;
import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicioId;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.RegraVicioId;
import com.josecjuniors.logossrv.core.vicio.domain.model.Vicio;
import com.josecjuniors.logossrv.core.vicio.domain.model.VicioId;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** PostgreSQL characterization of the actual child-state production services. */
@FreshPostgresIntegrationTest
@Import(ChildStateConcurrencyCharacterizationPostgresTest.SynchronousAsyncConfiguration.class)
class ChildStateConcurrencyCharacterizationPostgresTest {
    @Autowired CreateRegistroVicioService createRegistro;
    @Autowired ProcessarRegistroVicioService processarRegistro;
    @Autowired AppUserJpaRepository users;
    @Autowired JogadorJpaRepository jogadores;
    @Autowired VicioJpaRepository vicios;
    @Autowired DebuffJpaRepository debuffs;
    @Autowired RegraVicioJpaRepository regras;
    @Autowired VicioJogadorJpaRepository viciosJogador;
    @Autowired DebuffJogadorJpaRepository debuffsJogador;
    @Autowired RegistroVicioJpaRepository registros;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        registros.deleteAll();
        debuffsJogador.deleteAll();
        viciosJogador.deleteAll();
        regras.deleteAll();
        debuffs.deleteAll();
        vicios.deleteAll();
        jogadores.deleteAll();
        users.deleteAll();
    }

    @AfterEach
    void cleanAfter() {
        clean();
    }

    @Test
    void concurrentCreateRegistroVicioUsesOneLogicalVicioJogador() throws Exception {
        var player = createPlayer("child-create@example.test");
        var vicio = vicios.saveAndFlush(new Vicio(VicioId.generate(), "Create concurrency", null));

        runConcurrently(
                () -> createRegistro.create(new CreateRegistroVicioCommand(
                        "child-create@example.test", vicio.getId(), LocalDateTime.now(), "concurrent create one")),
                () -> createRegistro.create(new CreateRegistroVicioCommand(
                        "child-create@example.test", vicio.getId(), LocalDateTime.now(), "concurrent create two")));

        assertThat(count("select count(*) from vicio_jogador where jogador_id = ? and vicio_id = ?",
                player.getId().getValue(), vicio.getId().getValue())).isEqualTo(1);
        assertThat(count("select count(*) from registro_vicio rv join vicio_jogador vj on rv.vicio_jogador_id = vj.id where vj.jogador_id = ? and vj.vicio_id = ?",
                player.getId().getValue(), vicio.getId().getValue())).isEqualTo(2);
    }

    @Test
    void concurrentProcessarRegistroVicioAccumulatesOneLogicalDebuffJogador() throws Exception {
        var player = createPlayer("child-process@example.test");
        var debuff = debuffs.saveAndFlush(new Debuff(DebuffId.generate(), "Process concurrency"));
        var vicio = vicios.saveAndFlush(new Vicio(VicioId.generate(), "Process vicio", null));
        regras.saveAndFlush(new RegraVicio(RegraVicioId.generate(), vicio, null, 7, 24, null, debuff));
        var vicioJogador = viciosJogador.saveAndFlush(new VicioJogador(VicioJogadorId.generate(), player, vicio));
        var first = registros.saveAndFlush(new RegistroVicio(RegistroVicioId.generate(), vicioJogador, LocalDateTime.now(), "first"));
        var second = registros.saveAndFlush(new RegistroVicio(RegistroVicioId.generate(), vicioJogador, LocalDateTime.now(), "second"));

        runConcurrently(() -> processarRegistro.processar(new RegistroVicioCriadoEvent(first.getId(), player.getId())),
                () -> processarRegistro.processar(new RegistroVicioCriadoEvent(second.getId(), player.getId())));

        assertThat(count("select count(*) from debuff_jogador where jogador_id = ? and debuff_id = ?",
                player.getId().getValue(), debuff.getId().getValue())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select potencia from debuff_jogador where jogador_id = ? and debuff_id = ?",
                Integer.class, player.getId().getValue(), debuff.getId().getValue())).isEqualTo(14);
        assertThat(jdbc.queryForObject("select data_expiracao is not null from debuff_jogador where jogador_id = ? and debuff_id = ?",
                Boolean.class, player.getId().getValue(), debuff.getId().getValue())).isTrue();
    }

    private Jogador createPlayer(String email) {
        var user = users.saveAndFlush(new AppUser(new AppUserId(), email, encoder.encode("password")));
        return jogadores.saveAndFlush(new Jogador(JogadorId.generate(), user, email));
    }

    private int count(String sql, UUID jogadorId, UUID childId) {
        return jdbc.queryForObject(sql, Integer.class, jogadorId, childId);
    }

    private void runConcurrently(ThrowingRunnable... callers) throws Exception {
        var ready = new CountDownLatch(callers.length);
        var start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(callers.length);
        try {
            Future<?>[] futures = new Future<?>[callers.length];
            for (int index = 0; index < callers.length; index++) {
                var caller = callers[index];
                futures[index] = pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                    caller.run();
                    return null;
                });
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }

    @TestConfiguration
    static class SynchronousAsyncConfiguration {
        @Bean(name = "taskExecutor")
        TaskExecutor taskExecutor() { return new SyncTaskExecutor(); }
    }
}
