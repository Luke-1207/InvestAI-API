CREATE TABLE usuarios (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          nome VARCHAR(150) NOT NULL,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          senha VARCHAR(255) NOT NULL,
                          role VARCHAR(20) NOT NULL DEFAULT 'USUARIO',
                          ativo BOOLEAN NOT NULL DEFAULT TRUE,
                          deletado_em TIMESTAMP,
                          criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
                          atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                usuario_id UUID NOT NULL REFERENCES usuarios(id),
                                token VARCHAR(512) NOT NULL UNIQUE,
                                revogado BOOLEAN NOT NULL DEFAULT FALSE,
                                expira_em TIMESTAMP NOT NULL,
                                criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE perfil_investidor (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id),
                                   perfil_risco VARCHAR(20),
                                   horizonte VARCHAR(20),
                                   objetivo VARCHAR(30),
                                   valor_disponivel DECIMAL(15, 2),
                                   tipos_aceitos JSONB,
                                   setores_preferidos JSONB,
                                   perfil_preenchido BOOLEAN NOT NULL DEFAULT FALSE,
                                   criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
                                   atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);