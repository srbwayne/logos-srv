-- V29__Add_DiasSemVicio_To_VicioJogador.sql

ALTER TABLE vicio_jogador
ADD COLUMN dias_sem_vicio INT NOT NULL DEFAULT 0;
