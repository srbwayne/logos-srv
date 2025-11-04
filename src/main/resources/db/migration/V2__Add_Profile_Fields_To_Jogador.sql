-- V2__Add_Profile_Fields_To_Jogador.sql

-- Renomeia a coluna existente para 'apelido' e adiciona a constraint UNIQUE
ALTER TABLE jogador RENAME COLUMN nome_exibicao TO apelido;
ALTER TABLE jogador ADD CONSTRAINT uq_jogador_apelido UNIQUE (apelido);

-- Adiciona as novas colunas de perfil à tabela jogador
ALTER TABLE jogador ADD COLUMN nome_completo VARCHAR(255);
ALTER TABLE jogador ADD COLUMN data_nascimento DATE;
ALTER TABLE jogador ADD COLUMN cpf VARCHAR(14);
ALTER TABLE jogador ADD COLUMN numero_telefone VARCHAR(20);
ALTER TABLE jogador ADD COLUMN idade INT;
ALTER TABLE jogador ADD COLUMN sexo VARCHAR(50);
ALTER TABLE jogador ADD COLUMN descricao TEXT;
ALTER TABLE jogador ADD COLUMN endereco_pais VARCHAR(255);
ALTER TABLE jogador ADD COLUMN endereco_estado VARCHAR(2);
ALTER TABLE jogador ADD COLUMN endereco_cidade VARCHAR(255);
ALTER TABLE jogador ADD COLUMN endereco_descricao VARCHAR(255);
ALTER TABLE jogador ADD COLUMN endereco_complemento VARCHAR(255);
ALTER TABLE jogador ADD COLUMN endereco_cep VARCHAR(10);
