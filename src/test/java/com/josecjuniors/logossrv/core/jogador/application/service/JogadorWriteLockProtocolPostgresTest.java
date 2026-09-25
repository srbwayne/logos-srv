package com.josecjuniors.logossrv.core.jogador.application.service;

import com.josecjuniors.logossrv.adapters.out.appuser.jpa.AppUserJpaRepository;
import com.josecjuniors.logossrv.adapters.out.jogador.jpa.JogadorJpaRepository;
import com.josecjuniors.logossrv.adapters.out.progression.identity.jpa.ProgressionSubjectIdentityJpaRepository;
import com.josecjuniors.logossrv.config.jwt.JwtService;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUser;
import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobal;
import com.josecjuniors.logossrv.core.estresseglobal.domain.model.EstresseGlobalId;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdateApelidoCommand;
import com.josecjuniors.logossrv.core.jogador.application.port.in.UpdatePerfilJogadorCommand;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.support.test.FreshPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@FreshPostgresIntegrationTest
class JogadorWriteLockProtocolPostgresTest {
    @Autowired AppUserJpaRepository users;
    @Autowired JogadorJpaRepository jogadores;
    @Autowired ProgressionSubjectIdentityJpaRepository identities;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired UpdateApelidoService updateApelido;
    @Autowired UpdatePerfilJogadorService updatePerfil;

    @BeforeEach
    void clean() {
        identities.deleteAll();
        jogadores.deleteAll();
        users.deleteAll();
    }

    @Test
    void apelidoWriterWaitsForJogadorLock() throws Exception {
        var player = createPlayer("lock-apelido@example.test", "before");
        var acquired = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var holder = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jogadores.findByIdForUpdate(player.getId()).orElseThrow();
                acquired.countDown();
                await(release);
            }));
            assertThat(acquired.await(10, TimeUnit.SECONDS)).isTrue();
            var writer = pool.submit(() -> updateApelido.updateApelido(new UpdateApelidoCommand("lock-apelido@example.test", "after")));
            Thread.yield();
            assertThat(writer.isDone()).isFalse();
            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
            writer.get(10, TimeUnit.SECONDS);
            assertThat(jogadores.findById(player.getId()).orElseThrow().getApelido()).isEqualTo("after");
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void lockedWriterAndApelidoWriterPreserveBothStateChanges() throws Exception {
        var player = createPlayer("lock-regression@example.test", "before");
        var flushed = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var progressionWriter = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                var locked = jogadores.findByIdForUpdate(player.getId()).orElseThrow();
                locked.setXpTotal(30L);
                jogadores.saveAndFlush(locked);
                flushed.countDown();
                await(release);
            }));
            assertThat(flushed.await(10, TimeUnit.SECONDS)).isTrue();
            var profileWriter = pool.submit(() -> updateApelido.updateApelido(
                    new UpdateApelidoCommand("lock-regression@example.test", "after")));
            release.countDown();
            progressionWriter.get(10, TimeUnit.SECONDS);
            profileWriter.get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
        var finalPlayer = jogadores.findById(player.getId()).orElseThrow();
        assertThat(finalPlayer.getXpTotal()).isEqualTo(30L);
        assertThat(finalPlayer.getApelido()).isEqualTo("after");
    }

    @Test
    void stressDeltasComposeThroughJogadorLock() throws Exception {
        var player = createPlayer("lock-stress@example.test", "stress");
        var firstCommitted = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                var locked = jogadores.findByIdForUpdate(player.getId()).orElseThrow();
                locked.aplicarEstresse(5);
                jogadores.saveAndFlush(locked);
                firstCommitted.countDown();
                await(release);
            }));
            assertThat(firstCommitted.await(10, TimeUnit.SECONDS)).isTrue();
            var second = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                var locked = jogadores.findByIdForUpdate(player.getId()).orElseThrow();
                locked.aplicarEstresse(3);
                jogadores.saveAndFlush(locked);
            }));
            release.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
        assertThat(jogadores.findById(player.getId()).orElseThrow().getEstresseGlobal().getPontuacaoAtual())
                .isEqualTo(8);
    }

    @Test
    void perfilWriterUsesSameLockedLoad() {
        var player = createPlayer("lock-perfil@example.test", "perfil");
        updatePerfil.updatePerfil(new UpdatePerfilJogadorCommand(
                "lock-perfil@example.test", "Updated", null, null, null, null,
                "description", null, null, null, null, null, null));
        assertThat(jogadores.findById(player.getId()).orElseThrow().getNomeCompleto()).isEqualTo("Updated");
    }

    private Jogador createPlayer(String email, String nickname) {
        var user = users.saveAndFlush(new AppUser(new AppUserId(), email, encoder.encode("password")));
        var player = new Jogador(JogadorId.generate(), user, nickname);
        player.setEstresseGlobal(new EstresseGlobal(EstresseGlobalId.generate(), player));
        return jogadores.saveAndFlush(player);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("timed out waiting for lock release");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
