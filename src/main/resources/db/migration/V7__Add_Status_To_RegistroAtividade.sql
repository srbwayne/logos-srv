-- V7__Add_Status_To_RegistroAtividade.sql

-- Renomeia a coluna existente para refletir o novo significado
ALTER TABLE registro_atividade RENAME COLUMN data_hora TO data_hora_inicio;

-- Adiciona as novas colunas para gerenciamento de estado da atividade
ALTER TABLE registro_atividade ADD COLUMN situacao VARCHAR(50) NOT NULL DEFAULT 'INICIADA';
ALTER TABLE registro_atividade ADD COLUMN data_hora_fim TIMESTAMP;
ALTER TABLE registro_atividade ADD COLUMN hora_acumulada BIGINT; -- O tipo Duration é mapeado para um BIGINT (nanossegundos)
