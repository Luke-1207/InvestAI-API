CREATE TABLE titulo_tesouro (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                codigo VARCHAR(50) NOT NULL UNIQUE,
                                nome VARCHAR(150) NOT NULL,
                                tipo VARCHAR(15) NOT NULL,
                                taxa_anual DECIMAL(8,4) NOT NULL,
                                preco_minimo DECIMAL(15,2) NOT NULL,
                                vencimento DATE NOT NULL,
                                paga_juros_semestrais BOOLEAN NOT NULL DEFAULT FALSE,
                                disponivel BOOLEAN NOT NULL DEFAULT TRUE,
                                sincronizado_em TIMESTAMP
);

CREATE TABLE titulo_privado (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                tipo VARCHAR(10) NOT NULL,
                                emissor VARCHAR(150) NOT NULL,
                                indexador VARCHAR(15) NOT NULL,
                                taxa_percentual DECIMAL(8,4) NOT NULL,
                                vencimento DATE NOT NULL,
                                investimento_minimo DECIMAL(15,2) NOT NULL,
                                liquidez VARCHAR(20) NOT NULL,
                                garantido_fgc BOOLEAN NOT NULL DEFAULT TRUE,
                                isento_ir BOOLEAN NOT NULL DEFAULT FALSE,
                                ativo BOOLEAN NOT NULL DEFAULT TRUE,
                                criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
                                atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_titulo_tesouro_disponivel ON titulo_tesouro(disponivel);
CREATE INDEX idx_titulo_privado_ativo ON titulo_privado(ativo);