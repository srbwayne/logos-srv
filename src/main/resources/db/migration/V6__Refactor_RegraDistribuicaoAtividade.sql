-- V6__Refactor_RegraDistribuicaoAtividade.sql

-- Remove a constraint de chave estrangeira antiga que apontava para atributo_jogador
ALTER TABLE regra_distribuicao_atividade DROP CONSTRAINT IF EXISTS fk_regradistatividade_atributojogador;

-- Renomeia a coluna para refletir a nova associação
ALTER TABLE regra_distribuicao_atividade RENAME COLUMN atributo_jogador_id TO atributo_id;

-- Adiciona a nova constraint de chave estrangeira que aponta para a tabela de configuração 'atributo'
ALTER TABLE regra_distribuicao_atividade ADD CONSTRAINT fk_regradistatividade_atributo FOREIGN KEY (atributo_id) REFERENCES atributo(id);

-- Renomeia coluna atividade_id para atividade_config_id
ALTER TABLE regra_distribuicao_atividade RENAME COLUMN atividade_id TO atividade_config_id;