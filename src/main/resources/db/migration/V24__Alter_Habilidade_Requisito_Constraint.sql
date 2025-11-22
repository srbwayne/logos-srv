-- V24__Alter_Habilidade_Requisito_Constraint.sql

-- Primeiro, removemos a constraint antiga
ALTER TABLE habilidade_requisito DROP CONSTRAINT chk_requisito_tipo;

-- Adicionamos a nova constraint que inclui a verificação para o tipo JOGADOR
ALTER TABLE habilidade_requisito ADD CONSTRAINT chk_requisito_tipo CHECK (
    (tipo_requisito = 'ATRIBUTO' AND atributo_requisito_id IS NOT NULL AND habilidade_requisito_id IS NULL) OR
    (tipo_requisito = 'HABILIDADE' AND habilidade_requisito_id IS NOT NULL AND atributo_requisito_id IS NULL) OR
    (tipo_requisito = 'JOGADOR' AND atributo_requisito_id IS NULL AND habilidade_requisito_id IS NULL)
);
