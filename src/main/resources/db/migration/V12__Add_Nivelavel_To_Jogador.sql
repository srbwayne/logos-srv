-- V12__Add_Nivelavel_To_Jogador.sql

-- Adiciona as colunas para o sistema de nível e XP do jogador
ALTER TABLE jogador ADD COLUMN nivel_atual INT NOT NULL DEFAULT 1;
ALTER TABLE jogador ADD COLUMN xp_total BIGINT NOT NULL DEFAULT 0;
