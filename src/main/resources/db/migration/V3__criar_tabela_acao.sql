CREATE TABLE acao (
                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      codigo VARCHAR(10) NOT NULL UNIQUE,
                      nome VARCHAR(150) NOT NULL,
                      tipo VARCHAR(10) NOT NULL,
                      setor VARCHAR(100),
                      ativo BOOLEAN NOT NULL DEFAULT TRUE,
                      criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
                      atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_acao_codigo ON acao(codigo);
CREATE INDEX idx_acao_tipo ON acao(tipo);
CREATE INDEX idx_acao_setor ON acao(setor);