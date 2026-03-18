-- V31__Create_ConexaoExterna_Table.sql

CREATE TABLE conexao_externa (
    id UUID PRIMARY KEY,
    app_user_id UUID NOT NULL,
    provedor VARCHAR(255) NOT NULL,
    identificador_externo VARCHAR(255) NOT NULL,
    token_acesso VARCHAR(1024),
    token_atualizacao VARCHAR(1024),
    data_expiracao TIMESTAMP,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_conexao_externa_app_user FOREIGN KEY (app_user_id) REFERENCES app_user(id),
    CONSTRAINT uk_conexao_externa_app_user_provedor UNIQUE (app_user_id, provedor)
);
