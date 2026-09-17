package com.josecjuniors.logossrv.core.progression.application.service;

import com.josecjuniors.logossrv.core.progression.application.port.out.ProgressionExecutionReadPort;
import com.josecjuniors.logossrv.core.progression.application.query.ProgressionExecutionRead;
import com.josecjuniors.logossrv.core.progression.domain.exception.ProgressionExecutionNotFoundException;
import com.josecjuniors.logossrv.core.progression.domain.model.ProgressionExecutionIdentity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetProgressionExecutionServiceTest {
    private final ProgressionExecutionReadPort port = mock(ProgressionExecutionReadPort.class);
    private final GetProgressionExecutionService service = new GetProgressionExecutionService(port);

    @Test
    void convertsReadPortAbsenceToStableNotFoundException() {
        var identity = new ProgressionExecutionIdentity("lifeos", "missing");
        when(port.find(identity)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(identity))
                .isInstanceOf(ProgressionExecutionNotFoundException.class)
                .hasMessage("Progression execution was not found.");
        verify(port).find(identity);
    }

    @Test
    void returnsReadModelFromDedicatedPort() {
        var identity = new ProgressionExecutionIdentity("lifeos", "one");
        var read = mock(ProgressionExecutionRead.class);
        when(port.find(identity)).thenReturn(Optional.of(read));

        org.assertj.core.api.Assertions.assertThat(service.get(identity)).isSameAs(read);
        verify(port).find(identity);
    }
}
