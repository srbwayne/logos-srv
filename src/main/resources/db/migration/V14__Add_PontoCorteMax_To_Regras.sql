-- V14__Add_PontoCorteMax_To_Regras.sql

-- Adiciona a coluna para a "faixa ótima" na regra de XP
ALTER TABLE regra_fator_xp
ADD COLUMN ponto_corte_max DOUBLE PRECISION;

-- Adiciona a coluna para a "faixa ótima" na regra de Estresse
ALTER TABLE regra_fator_estresse
ADD COLUMN ponto_corte_max DOUBLE PRECISION;
