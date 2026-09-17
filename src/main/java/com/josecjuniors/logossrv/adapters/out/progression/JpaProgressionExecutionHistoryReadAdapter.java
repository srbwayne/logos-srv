package com.josecjuniors.logossrv.adapters.out.progression;

import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionExecutionHistoryReadPort;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryItem;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPage;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionHistoryPageRequest;
import com.josecjuniors.logossrv.core.progression.domain.model.ExternalSubjectReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProgressionExecutionHistoryReadAdapter implements ProgressionExecutionHistoryReadPort {
    private final ProgressionExternalExecutionJpaRepository repository;
    private final ProgressionExecutionReadMapper mapper;

    public JpaProgressionExecutionHistoryReadAdapter(ProgressionExternalExecutionJpaRepository repository,
                                                     ProgressionExecutionReadMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ProgressionExecutionHistoryPage find(ExternalSubjectReference subject,
                                                ProgressionExecutionHistoryPageRequest request) {
        var page = repository.findHistory(subject.namespace(), subject.externalId(),
                PageRequest.of(request.page(), request.size()));
        var items = page.getContent().stream()
                .map(entity -> new ProgressionExecutionHistoryItem(mapper.map(entity), entity.getOccurredAt()))
                .toList();
        return new ProgressionExecutionHistoryPage(items, request.page(), request.size(),
                page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }
}
