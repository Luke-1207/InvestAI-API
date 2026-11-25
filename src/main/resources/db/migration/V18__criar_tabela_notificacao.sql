CREATE TABLE notificacao (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo       VARCHAR(30)  NOT NULL,
    titulo     VARCHAR(150) NOT NULL,
    mensagem   TEXT         NOT NULL,
    ativo_id   UUID,
    tipo_ativo VARCHAR(20),
    lida       BOOLEAN      NOT NULL DEFAULT FALSE,
    lida_em    TIMESTAMP,
    criado_em  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_notificacao_ativo_completo CHECK ((ativo_id IS NULL) = (tipo_ativo IS NULL))
);

CREATE INDEX idx_notificacao_usuario_lida_criado_em
    ON notificacao (usuario_id, lida, criado_em DESC);
