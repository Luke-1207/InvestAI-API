CREATE TABLE status_integracao (
    integracao VARCHAR(30) PRIMARY KEY,
    ultimo_sucesso_em TIMESTAMP,
    ultima_falha_em TIMESTAMP,
    ultimo_erro VARCHAR(500)
);
