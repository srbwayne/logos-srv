-- V28__Refactor_RegistroVicio_Table.sql

-- Remove as colunas antigas e suas constraints
ALTER TABLE registro_vicio DROP CONSTRAINT IF EXISTS fk_registrovicio_jogador;
ALTER TABLE registro_vicio DROP CONSTRAINT IF EXISTS fk_registrovicio_regravicio;
ALTER TABLE registro_vicio DROP COLUMN IF EXISTS jogador_id;
ALTER TABLE registro_vicio DROP COLUMN IF EXISTS regra_vicio_id;
ALTER TABLE registro_vicio DROP COLUMN IF EXISTS esta_ativo;

-- Adiciona as novas colunas
ALTER TABLE registro_vicio ADD COLUMN vicio_jogador_id UUID NOT NULL;
ALTER TABLE registro_vicio ADD COLUMN observacao TEXT;

-- Adiciona a nova constraint de chave estrangeira
ALTER TABLE registro_vicio ADD CONSTRAINT fk_registrovicio_viciojogador FOREIGN KEY (vicio_jogador_id) REFERENCES vicio_jogador(id);
