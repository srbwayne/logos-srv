package com.josecjuniors.logossrv.adapters.out.jogador.jpa;

import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogador;
import com.josecjuniors.logossrv.core.jogador.domain.model.AtributoJogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.model.JogadorId;
import com.josecjuniors.logossrv.core.jogador.domain.repository.AtributoJogadorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface AtributoJogadorJpaRepository extends AtributoJogadorRepository, JpaRepository<AtributoJogador, AtributoJogadorId> {

    /**
     * Busca os atributos de um jogador de forma paginada, utilizando JOIN FETCH
     * para evitar o problema de N+1 queries ao acessar os dados do Atributo relacionado.
     *
     * @param email O email do usuário associado ao jogador.
     * @param pageable As informações de paginação.
     * @return Uma página de AtributoJogador com os Atributos já carregados.
     */
    @Override
    @Query("SELECT aj FROM AtributoJogador aj JOIN FETCH aj.atributo a WHERE aj.jogador.user.email = :email")
    Page<AtributoJogador> findByJogadorUserEmail(@Param("email") String email, Pageable pageable);

    @Override
    Set<AtributoJogador> findByJogadorId(JogadorId jogadorId);
}
