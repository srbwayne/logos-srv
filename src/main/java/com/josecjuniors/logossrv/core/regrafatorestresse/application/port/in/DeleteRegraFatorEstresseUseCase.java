package com.josecjuniors.logossrv.core.regrafatorestresse.application.port.in;

import com.josecjuniors.logossrv.core.regrafatorestresse.domain.model.RegraFatorEstresseId;

public interface DeleteRegraFatorEstresseUseCase {
    void delete(RegraFatorEstresseId id);
}
