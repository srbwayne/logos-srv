-- V10__Create_RegistroAtividadeDetalhe_Table.sql

CREATE TABLE registro_atividade_detalhe (
    id UUID PRIMARY KEY,
    registro_atividade_id UUID NOT NULL,
    fator_calculo_id UUID NOT NULL,
    valor_registrado DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_detalhe_registro_atividade FOREIGN KEY (registro_atividade_id) REFERENCES registro_atividade(id),
    CONSTRAINT fk_detalhe_fator_calculo FOREIGN KEY (fator_calculo_id) REFERENCES fator_calculo(id)
);
