package com.josecjuniors.logossrv.core.registrovicio.application.service;

import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.VicioJogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.DebuffJogadorRepository;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.registrovicio.domain.events.RegistroVicioCriadoEvent;
import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicio;
import com.josecjuniors.logossrv.core.registrovicio.domain.model.RegistroVicioId;
import com.josecjuniors.logossrv.core.registrovicio.domain.repository.RegistroVicioRepository;
import com.josecjuniors.logossrv.core.vicio.application.service.NivelVicioService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessarRegistroVicioLockOrderTest {
    @Test
    void locksJogadorBeforeMutatingVicioState() {
        var registros = mock(RegistroVicioRepository.class);
        var debuffs = mock(DebuffJogadorRepository.class);
        var nivel = mock(NivelVicioService.class);
        var jogadores = mock(JogadorRepository.class);
        var registro = mock(RegistroVicio.class);
        var vicioJogador = mock(VicioJogador.class);
        var staleJogador = mock(Jogador.class);
        var lockedJogador = mock(Jogador.class);
        var jogadorId = JogadorId.generate();
        var registroId = RegistroVicioId.generate();
        var event = new RegistroVicioCriadoEvent(registroId, jogadorId);

        when(registros.findById(registroId)).thenReturn(Optional.of(registro));
        when(registro.getVicioJogador()).thenReturn(vicioJogador);
        when(vicioJogador.getJogador()).thenReturn(staleJogador);
        when(staleJogador.getId()).thenReturn(jogadorId);
        when(jogadores.findByIdForUpdate(jogadorId)).thenReturn(Optional.of(lockedJogador));
        when(vicioJogador.getVicio()).thenReturn(mock(com.josecjuniors.logossrv.core.vicio.domain.model.Vicio.class));
        when(vicioJogador.getVicio().getRegras()).thenReturn(Set.of());

        new ProcessarRegistroVicioService(registros, debuffs, nivel, jogadores).processar(event);

        var order = inOrder(jogadores, vicioJogador);
        order.verify(jogadores).findByIdForUpdate(jogadorId);
        order.verify(vicioJogador).registrarRecaida();
    }
}
