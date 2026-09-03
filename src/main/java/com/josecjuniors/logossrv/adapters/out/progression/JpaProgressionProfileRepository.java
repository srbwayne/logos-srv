package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.appuser.domain.model.AppUserId;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.jogador.domain.model.Jogador;
import com.josecjuniors.logossrv.core.jogador.domain.repository.JogadorRepository;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionProfileRepository;
import com.josecjuniors.logossrv.core.progression.domain.model.SubjectId;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfile;
import com.josecjuniors.logossrv.core.registroatividade.application.service.ProgressionProfileMapper;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionSubjectNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adapta temporariamente SubjectId ao vínculo persistente Jogador.user_id. */
@Repository
public class JpaProgressionProfileRepository implements ProgressionProfileRepository {

    private final JogadorRepository jogadorRepository;
    private final AtributoRepository atributoRepository;
    private final ProgressionProfileMapper mapper = new ProgressionProfileMapper();

    public JpaProgressionProfileRepository(JogadorRepository jogadorRepository,
                                           AtributoRepository atributoRepository) {
        this.jogadorRepository = jogadorRepository;
        this.atributoRepository = atributoRepository;
    }

    @Override
    public Optional<ProgressionProfile> findBySubjectId(SubjectId subjectId) {
        return jogadorRepository.findByAppUserId(new AppUserId(subjectId.value()))
                .map(mapper::from);
    }

    @Override
    public ProgressionProfile save(SubjectId subjectId, ProgressionProfile profile) {
        Jogador jogador = jogadorRepository.findByAppUserId(new AppUserId(subjectId.value()))
                .orElseThrow(ProgressionSubjectNotFoundException::new);
        mapper.applyTo(jogador, profile, atributoRepository.findAll());
        jogadorRepository.save(jogador);
        return mapper.from(jogador);
    }
}
