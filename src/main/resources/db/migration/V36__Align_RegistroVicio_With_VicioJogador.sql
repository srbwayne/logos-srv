ALTER TABLE registro_vicio
    DROP CONSTRAINT IF EXISTS fk_registrovicio_vicio;

ALTER TABLE registro_vicio
    DROP COLUMN IF EXISTS vicio_id;
