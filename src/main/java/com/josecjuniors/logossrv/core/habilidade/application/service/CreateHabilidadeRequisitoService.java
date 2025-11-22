package com.josecjuniors.logossrv.core.habilidade.application.service;

import com.josecjuniors.logossrv.core.atributo.domain.exception.AtributoNaoEncontradoException;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.model.AtributoId;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import com.josecjuniors.logossrv.core.habilidade.application.dto.HabilidadeRequisitoDto;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.commands.CreateHabilidadeRequisitoCommand;
import com.josecjuniors.logossrv.core.habilidade.application.port.in.CreateHabilidadeRequisitoUseCase;
import com.josecjuniors.logossrv.core.habilidade.domain.exception.HabilidadeNaoEncontradaException;
import com.josecjuniors.logossrv.core.habilidade.domain.model.Habilidade;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeId;
import com.josecjuniors.logossrv.core.habilidade.domain.model.HabilidadeRequisito;
import com.josecjuniors.logossrv.core.habilidade.domain.model.enums.TipoRequisito;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRepository;
import com.josecjuniors.logossrv.core.habilidade.domain.repository.HabilidadeRequisitoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateHabilidadeRequisitoService implements CreateHabilidadeRequisitoUseCase {

    private final HabilidadeRepository habilidadeRepository;
    private final AtributoRepository atributoRepository;
    private final HabilidadeRequisitoRepository requisitoRepository;

    public CreateHabilidadeRequisitoService(HabilidadeRepository habilidadeRepository, AtributoRepository atributoRepository, HabilidadeRequisitoRepository requisitoRepository) {
        this.habilidadeRepository = habilidadeRepository;
        this.atributoRepository = atributoRepository;
        this.requisitoRepository = requisitoRepository;
    }

    @Override
    public HabilidadeRequisitoDto create(UUID habilidadeId, TipoRequisito tipo, UUID requisitoId, Integer nivelMinimo) {
        Habilidade habilidade = habilidadeRepository.findById(new HabilidadeId(habilidadeId))
                .orElseThrow(HabilidadeNaoEncontradaException::new);

        Atributo atributoReq = null;
        Habilidade habilidadeReq = null;

        if (tipo == TipoRequisito.ATRIBUTO) {
            atributoReq = atributoRepository.findById(new AtributoId(requisitoId))
                    .orElseThrow(AtributoNaoEncontradoException::new);
        } else if (tipo == TipoRequisito.HABILIDADE) {
            habilidadeReq = habilidadeRepository.findById(new HabilidadeId(requisitoId))
                    .orElseThrow(HabilidadeNaoEncontradaException::new);
        }

        var command = new CreateHabilidadeRequisitoCommand(habilidade, atributoReq, habilidadeReq, nivelMinimo);

        HabilidadeRequisito novoRequisito = tipo.criar(command);
        HabilidadeRequisito requisitoSalvo = requisitoRepository.save(novoRequisito);
        return HabilidadeRequisitoDto.fromDomain(requisitoSalvo);
    }
}
