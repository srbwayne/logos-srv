-- V11__Rename_Estresse_To_EstresseGlobal.sql

-- Renomeia a tabela 'estresse' para 'estresse_global' para corresponder ao nome da entidade Java.
-- As colunas e constraints (jogador_id, etc.) já existiam desde o script V1.
ALTER TABLE IF EXISTS estresse RENAME TO estresse_global;
