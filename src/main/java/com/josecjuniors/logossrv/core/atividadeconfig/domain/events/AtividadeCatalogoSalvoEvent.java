package com.josecjuniors.logossrv.core.atividadeconfig.domain.events;

import com.josecjuniors.logossrv.core.atividadeconfig.domain.model.AtividadeConfigId;

/** Event emitted when Activity Catalog metadata changes. */
public record AtividadeCatalogoSalvoEvent(AtividadeConfigId atividadeConfigId) {}
