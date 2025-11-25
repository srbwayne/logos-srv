-- V26__Create_Debuff_And_RegraVicio_Tables.sql

-- Tabela de Definição de Debuffs (deve ser criada primeiro)
CREATE TABLE debuff (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE
);

-- Tabela de Regras de Vício (agora incluindo sua criação)
CREATE TABLE regra_vicio (
    id UUID PRIMARY KEY,
    vicio_id UUID NOT NULL,
    impacto_estresse INT,
    penalidade_pontos INT,
    duracao_horas INT,
    debuff_id UUID,
    CONSTRAINT fk_regravicio_vicio FOREIGN KEY (vicio_id) REFERENCES vicio(id),
    CONSTRAINT fk_regravicio_debuff FOREIGN KEY (debuff_id) REFERENCES debuff(id)
);

-- Tabela de Regras de Distribuição de Debuffs
CREATE TABLE regra_distribuicao_debuff (
    id UUID PRIMARY KEY,
    debuff_id UUID NOT NULL,
    atributo_id UUID NOT NULL,
    CONSTRAINT fk_regra_debuff_debuff FOREIGN KEY (debuff_id) REFERENCES debuff(id),
    CONSTRAINT fk_regra_debuff_atributo FOREIGN KEY (atributo_id) REFERENCES atributo(id)
);

-- Tabela de Instâncias de Debuffs Ativos nos Jogadores
CREATE TABLE debuff_jogador (
    id UUID PRIMARY KEY,
    jogador_id UUID NOT NULL,
    debuff_id UUID NOT NULL,
    potencia INT NOT NULL,
    data_expiracao TIMESTAMP NOT NULL,
    CONSTRAINT fk_debuffjogador_jogador FOREIGN KEY (jogador_id) REFERENCES jogador(id),
    CONSTRAINT fk_debuffjogador_debuff FOREIGN KEY (debuff_id) REFERENCES debuff(id)
);
