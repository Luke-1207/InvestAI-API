CREATE TABLE usuario_foto (
                              usuario_id    UUID PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
                              conteudo      BYTEA        NOT NULL,
                              content_type  VARCHAR(50)  NOT NULL,
                              atualizado_em TIMESTAMP    NOT NULL DEFAULT NOW()
);