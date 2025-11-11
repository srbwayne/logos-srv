-- V13__Create_HabilidadeRequisito_Table.sql

CREATE TABLE habilidade_requisito (
    id UUID PRIMARY KEY,
    habilidade_id UUID NOT NULL,
    tipo_requisito VARCHAR(50) NOT NULL,
    atributo_requisito_id UUID,
    habilidade_requisito_id UUID,
    nivel_minimo INT NOT NULL,

    CONSTRAINT fk_habilidaderequisito_habilidade FOREIGN KEY (habilidade_id) REFERENCES habilidade(id),
    CONSTRAINT fk_habilidaderequisito_atributo_req FOREIGN KEY (atributo_requisito_id) REFERENCES atributo(id),
    CONSTRAINT fk_habilidaderequisito_habilidade_req FOREIGN KEY (habilidade_requisito_id) REFERENCES habilidade(id),

    -- Garante a integridade dos dados: ou o requisito é um atributo, ou é uma habilidade, mas não ambos.
    CONSTRAINT chk_requisito_tipo CHECK (
        (tipo_requisito = 'ATRIBUTO' AND atributo_requisito_id IS NOT NULL AND habilidade_requisito_id IS NULL) OR
        (tipo_requisito = 'HABILIDADE' AND habilidade_requisito_id IS NOT NULL AND atributo_requisito_id IS NULL)
    )
);
