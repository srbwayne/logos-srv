-- V8__Add_Penalty_Fields_To_AtividadeConfig.sql

-- Adiciona colunas para a futura funcionalidade de penalidade por inatividade
ALTER TABLE atividade_config ADD COLUMN dias_para_penalidade INT;
ALTER TABLE atividade_config ADD COLUMN xp_perda_por_ciclo INT;
