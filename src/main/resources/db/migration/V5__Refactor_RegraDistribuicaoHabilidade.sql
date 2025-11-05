-- V5__Refactor_RegraDistribuicaoHabilidade.sql

-- Remove as constraints de chave estrangeira antigas
ALTER TABLE regra_distribuicao_habilidade DROP CONSTRAINT IF EXISTS fk_regradisthabilidade_habilidadejogador;
ALTER TABLE regra_distribuicao_habilidade DROP CONSTRAINT IF EXISTS fk_regradisthabilidade_atributojogador;

-- Remove as colunas antigas
ALTER TABLE regra_distribuicao_habilidade DROP COLUMN IF EXISTS habilidade_jogador_id;
ALTER TABLE regra_distribuicao_habilidade DROP COLUMN IF EXISTS atributo_jogador_id;

-- Adiciona as novas colunas para apontar para as tabelas de configuração
ALTER TABLE regra_distribuicao_habilidade ADD COLUMN habilidade_id UUID NOT NULL;
ALTER TABLE regra_distribuicao_habilidade ADD COLUMN atributo_id UUID NOT NULL;

-- Adiciona as novas constraints de chave estrangeira
ALTER TABLE regra_distribuicao_habilidade ADD CONSTRAINT fk_regradisthabilidade_habilidade FOREIGN KEY (habilidade_id) REFERENCES habilidade(id);
ALTER TABLE regra_distribuicao_habilidade ADD CONSTRAINT fk_regradisthabilidade_atributo FOREIGN KEY (atributo_id) REFERENCES atributo(id);
