DROP TABLE regra_fator_xp;
DROP TABLE regra_fator_estresse;
DROP TABLE regra_distribuicao_atividade;

ALTER TABLE atividade_config DROP COLUMN xp_base;
ALTER TABLE atividade_config DROP COLUMN estresse_base;
ALTER TABLE atividade_config DROP COLUMN dias_para_penalidade;
ALTER TABLE atividade_config DROP COLUMN xp_perda_por_ciclo;
