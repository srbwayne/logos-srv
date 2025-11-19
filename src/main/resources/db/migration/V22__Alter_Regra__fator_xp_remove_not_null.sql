-- V22__Alter_Regra__fator_xp_remove_not_null.sql


-- Removendo not null pois posso adicionar valores náo calculaveis
ALTER TABLE regra_fator_xp ALTER COLUMN ponto_corte_min DROP NOT NULL;
ALTER TABLE regra_fator_xp ALTER COLUMN peso_multiplicador DROP NOT NULL;
