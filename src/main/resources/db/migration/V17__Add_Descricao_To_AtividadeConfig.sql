-- V17__Add_Descricao_To_AtividadeConfig.sql

ALTER TABLE atividade_config
ADD COLUMN descricao TEXT;
