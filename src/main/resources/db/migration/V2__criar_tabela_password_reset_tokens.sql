CREATE TABLE password_reset_tokens (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       usuario_id UUID NOT NULL REFERENCES usuarios(id),
                                       token VARCHAR(255) NOT NULL UNIQUE,
                                       usado BOOLEAN NOT NULL DEFAULT FALSE,
                                       expira_em TIMESTAMP NOT NULL,
                                       criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_tokens_usuario_id ON password_reset_tokens(usuario_id);