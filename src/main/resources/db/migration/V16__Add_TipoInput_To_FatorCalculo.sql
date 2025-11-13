-- V16__Add_TipoInput_To_FatorCalculo.sql

ALTER TABLE fator_calculo
ADD COLUMN tipo_input VARCHAR(50) NOT NULL DEFAULT 'NUMERICO';
