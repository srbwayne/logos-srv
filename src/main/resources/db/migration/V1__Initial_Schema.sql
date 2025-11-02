-- V1__Initial_Schema.sql
-- Script consolidado que define a estrutura inicial completa do banco de dados.

-- Tabelas de Configuração (Regras)
CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE atributo (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE habilidade (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE vicio (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE atividade_config (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    xp_base INT,
    estresse_base INT
);

CREATE TABLE fator_calculo (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    unidade_medida VARCHAR(50)
);

-- Tabelas de Estado (Instâncias por Jogador)
CREATE TABLE jogador (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    nome_exibicao VARCHAR(255) NOT NULL,
    CONSTRAINT fk_jogador_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE atributo_jogador (
    id UUID PRIMARY KEY,
    jogador_id UUID NOT NULL,
    atributo_id UUID NOT NULL,
    xp_total BIGINT NOT NULL,
    nivel_atual INT NOT NULL,
    atributo_penalizador_id UUID,
    CONSTRAINT fk_atributojogador_jogador FOREIGN KEY (jogador_id) REFERENCES jogador(id),
    CONSTRAINT fk_atributojogador_atributo FOREIGN KEY (atributo_id) REFERENCES atributo(id),
    CONSTRAINT fk_atributojogador_penalizador FOREIGN KEY (atributo_penalizador_id) REFERENCES atributo_jogador(id),
    CONSTRAINT uq_jogador_atributo UNIQUE (jogador_id, atributo_id)
);

CREATE TABLE habilidade_jogador (
    id UUID PRIMARY KEY,
    jogador_id UUID NOT NULL,
    habilidade_id UUID NOT NULL,
    xp_total BIGINT NOT NULL,
    nivel_atual INT NOT NULL,
    CONSTRAINT fk_habilidadejogador_jogador FOREIGN KEY (jogador_id) REFERENCES jogador(id),
    CONSTRAINT fk_habilidadejogador_habilidade FOREIGN KEY (habilidade_id) REFERENCES habilidade(id),
    CONSTRAINT uq_jogador_habilidade UNIQUE (jogador_id, habilidade_id)
);

CREATE TABLE vicio_jogador (
    id UUID PRIMARY KEY,
    jogador_id UUID NOT NULL,
    vicio_id UUID NOT NULL,
    esta_ativo BOOLEAN NOT NULL,
    data_inicio TIMESTAMP,
    xp_total BIGINT,
    nivel_atual INT,
    CONSTRAINT fk_viciojogador_jogador FOREIGN KEY (jogador_id) REFERENCES jogador(id),
    CONSTRAINT fk_viciojogador_vicio FOREIGN KEY (vicio_id) REFERENCES vicio(id)
);

-- Tabelas de Ligação e Regras Complexas
CREATE TABLE regra_distribuicao_atividade (
    id UUID PRIMARY KEY,
    atividade_id UUID NOT NULL,
    atributo_jogador_id UUID NOT NULL,
    peso_percentual DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_regradistatividade_atividade FOREIGN KEY (atividade_id) REFERENCES atividade_config(id),
    CONSTRAINT fk_regradistatividade_atributojogador FOREIGN KEY (atributo_jogador_id) REFERENCES atributo_jogador(id)
);

CREATE TABLE regra_distribuicao_habilidade (
    id UUID PRIMARY KEY,
    habilidade_jogador_id UUID NOT NULL,
    atributo_jogador_id UUID NOT NULL,
    peso_distribuicao DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_regradisthabilidade_habilidadejogador FOREIGN KEY (habilidade_jogador_id) REFERENCES habilidade_jogador(id),
    CONSTRAINT fk_regradisthabilidade_atributojogador FOREIGN KEY (atributo_jogador_id) REFERENCES atributo_jogador(id)
);

CREATE TABLE regra_fator_xp (
    id UUID PRIMARY KEY,
    regra_distribuicao_atividade_id UUID NOT NULL,
    fator_calculo_id UUID NOT NULL,
    peso_multiplicador DOUBLE PRECISION NOT NULL,
    ponto_corte_min DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_regrafatorxp_regradistatividade FOREIGN KEY (regra_distribuicao_atividade_id) REFERENCES regra_distribuicao_atividade(id),
    CONSTRAINT fk_regrafatorxp_fatorcalculo FOREIGN KEY (fator_calculo_id) REFERENCES fator_calculo(id)
);

-- Tabelas de Eventos
CREATE TABLE registro_atividade (
    id UUID PRIMARY KEY,
    jogador_id UUID NOT NULL,
    atividade_config_id UUID NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    xp_ganho_final INT,
    estresse_gerado INT,
    CONSTRAINT fk_registroatividade_jogador FOREIGN KEY (jogador_id) REFERENCES jogador(id),
    CONSTRAINT fk_registroatividade_atividadeconfig FOREIGN KEY (atividade_config_id) REFERENCES atividade_config(id)
);

CREATE TABLE registro_vicio (
    id UUID PRIMARY KEY,
    jogador_id UUID NOT NULL,
    vicio_id UUID NOT NULL, -- Ajustado para apontar para a nova tabela vicio
    data_hora TIMESTAMP NOT NULL,
    esta_ativo BOOLEAN,
    CONSTRAINT fk_registrovicio_jogador FOREIGN KEY (jogador_id) REFERENCES jogador(id),
    CONSTRAINT fk_registrovicio_vicio FOREIGN KEY (vicio_id) REFERENCES vicio(id)
);

CREATE TABLE estresse (
    id UUID PRIMARY KEY,
    jogador_id UUID NOT NULL UNIQUE,
    pontuacao_atual INT,
    CONSTRAINT fk_estresse_jogador FOREIGN KEY (jogador_id) REFERENCES jogador(id)
);
