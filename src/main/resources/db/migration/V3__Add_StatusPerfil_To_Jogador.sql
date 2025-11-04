-- V3__Add_StatusPerfil_To_Jogador.sql

ALTER TABLE jogador
ADD COLUMN status_perfil VARCHAR(50) NOT NULL DEFAULT 'INCOMPLETO';
