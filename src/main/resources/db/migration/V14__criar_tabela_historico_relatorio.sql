CREATE TABLE historico_relatorio (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID         NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo       VARCHAR(20)  NOT NULL,
    referencia VARCHAR(150) NOT NULL,
    gerado_em  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_historico_relatorio_usuario_gerado_em
    ON historico_relatorio (usuario_id, gerado_em DESC);
