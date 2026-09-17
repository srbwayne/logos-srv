package com.josecjuniors.logossrv.adapters.out.progression;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionExecutionReadPort;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionRead;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaProgressionExecutionReadAdapter implements ProgressionExecutionReadPort {
    private final ProgressionExternalExecutionJpaRepository repository;
    private final ProgressionExecutionReadMapper mapper;

    @Autowired
    public JpaProgressionExecutionReadAdapter(ProgressionExternalExecutionJpaRepository repository,
                                              ProgressionExecutionReadMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProgressionExecutionRead> find(ProgressionExecutionIdentity identity) {
        return repository.findBySourceSystemAndIdempotencyKey(identity.source(), identity.idempotencyKey())
                .map(mapper::map);
    }

    public JpaProgressionExecutionReadAdapter(ProgressionExternalExecutionJpaRepository repository,
                                              ObjectMapper objectMapper) {
        this(repository, new ProgressionExecutionReadMapper(objectMapper));
    }
}
