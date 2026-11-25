CREATE TABLE favorito (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id          UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    ativo_id            UUID         NOT NULL,
    tipo_ativo          VARCHAR(20)  NOT NULL,
    alertas_ativos      BOOLEAN      NOT NULL DEFAULT TRUE,
    tipos_alerta_ativos JSONB,
    limiar_variacao     NUMERIC(5,2) NOT NULL DEFAULT 5.00,
    criado_em           TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_favorito_limiar_variacao CHECK (limiar_variacao > 0 AND limiar_variacao <= 100)
);

CREATE UNIQUE INDEX uk_favorito_usuario_ativo_tipo
    ON favorito (usuario_id, ativo_id, tipo_ativo);
