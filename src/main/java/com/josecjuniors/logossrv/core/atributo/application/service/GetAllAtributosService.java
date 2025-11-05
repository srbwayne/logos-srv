package com.josecjuniors.logossrv.core.atributo.application.service;

import com.josecjuniors.logossrv.core.atributo.application.dto.AtributoDto;
import com.josecjuniors.logossrv.core.atributo.application.port.in.GetAllAtributosUseCase;
import com.josecjuniors.logossrv.core.atributo.domain.model.Atributo;
import com.josecjuniors.logossrv.core.atributo.domain.repository.AtributoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAllAtributosService implements GetAllAtributosUseCase {

    private final AtributoRepository atributoRepository;

    public GetAllAtributosService(AtributoRepository atributoRepository) {
        this.atributoRepository = atributoRepository;
    }

    @Override
    public Page<AtributoDto> getAll(String searchTerm, Pageable pageable) {
        Page<Atributo> atributoPage = atributoRepository.findByNomeContainingIgnoreCase(searchTerm, pageable);
        return atributoPage.map(this::toDto);
    }

    private AtributoDto toDto(Atributo atributo) {
        return new AtributoDto(
                atributo.getId().getValue().toString(),
                atributo.getNome(),
                atributo.getDescricao()
        );
    }
}
