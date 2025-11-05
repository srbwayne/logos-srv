-- V4__Add_Descricao_To_Atributo_And_Habilidade.sql

ALTER TABLE atributo
ADD COLUMN descricao TEXT;

ALTER TABLE habilidade
ADD COLUMN descricao TEXT;
