-- V19__Add_Status_Processamento_To_RegistroAtividade.sql



-- Adiciona as novas colunas para gerenciamento de processamento assincrono da atividade
ALTER TABLE registro_atividade ADD COLUMN status_processamento VARCHAR(50) NOT NULL DEFAULT 'PENDENTE';
ALTER TABLE registro_atividade ADD COLUMN data_registro TIMESTAMP NOT NULL DEFAULT now();

