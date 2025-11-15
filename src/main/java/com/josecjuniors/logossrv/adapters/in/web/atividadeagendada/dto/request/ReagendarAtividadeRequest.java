package com.josecjuniors.logossrv.adapters.in.web.atividadeagendada.dto.request;

import java.time.LocalDateTime;

public record ReagendarAtividadeRequest(
        LocalDateTime novaDataHoraInicio,
        LocalDateTime novaDataHoraFim
) {}
