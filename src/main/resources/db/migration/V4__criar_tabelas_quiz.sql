CREATE TABLE quiz_pergunta (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               ordem INT NOT NULL UNIQUE,
                               texto VARCHAR(255) NOT NULL,
                               tipo VARCHAR(20) NOT NULL,
                               campo_perfil VARCHAR(30) NOT NULL,
                               obrigatoria BOOLEAN NOT NULL DEFAULT TRUE,
                               ativa BOOLEAN NOT NULL DEFAULT TRUE,
                               criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
                               atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE quiz_opcao (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            quiz_pergunta_id UUID NOT NULL REFERENCES quiz_pergunta(id),
                            ordem INT NOT NULL,
                            texto VARCHAR(255) NOT NULL,
                            emoji VARCHAR(10),
                            mapeamento_json JSONB NOT NULL,
                            criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
                            atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
                            UNIQUE (quiz_pergunta_id, ordem)
);

CREATE INDEX idx_quiz_opcao_quiz_pergunta_id ON quiz_opcao(quiz_pergunta_id);

-- Pergunta 1 — Objetivo Financeiro
INSERT INTO quiz_pergunta (id, ordem, texto, tipo, campo_perfil, obrigatoria, ativa) VALUES
    ('a0000000-0000-4000-8000-000000000001', 1, 'O que você quer conquistar com seus investimentos?', 'UNICA_ESCOLHA', 'OBJETIVO_FINANCEIRO', TRUE, TRUE);

INSERT INTO quiz_opcao (quiz_pergunta_id, ordem, texto, emoji, mapeamento_json) VALUES
                                                                                    ('a0000000-0000-4000-8000-000000000001', 1, 'Receber uma renda todo mês, sem precisar vender nada', '💰', '{"objetivo": "RENDA_PASSIVA"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000001', 2, 'Fazer meu dinheiro crescer bastante ao longo do tempo', '📈', '{"objetivo": "CRESCIMENTO_PATRIMONIO"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000001', 3, 'Proteger o que já tenho e evitar perder dinheiro', '🛡️', '{"objetivo": "PRESERVAR_CAPITAL"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000001', 4, 'Juntar dinheiro para um objetivo específico (viagem, imóvel, aposentadoria...)', '✈️', '{"objetivo": "CRESCIMENTO_PATRIMONIO"}'::jsonb);

-- Pergunta 2 — Horizonte de Investimento
INSERT INTO quiz_pergunta (id, ordem, texto, tipo, campo_perfil, obrigatoria, ativa) VALUES
    ('a0000000-0000-4000-8000-000000000002', 2, 'Em quanto tempo você pensa em usar esse dinheiro?', 'UNICA_ESCOLHA', 'HORIZONTE_INVESTIMENTO', TRUE, TRUE);

INSERT INTO quiz_opcao (quiz_pergunta_id, ordem, texto, emoji, mapeamento_json) VALUES
                                                                                    ('a0000000-0000-4000-8000-000000000002', 1, 'Em menos de 1 ano — pode precisar logo', '⚡', '{"horizonte": "CURTO_PRAZO"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000002', 2, 'Entre 1 e 5 anos — tenho um plano de médio prazo', '🌱', '{"horizonte": "MEDIO_PRAZO"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000002', 3, 'Mais de 5 anos — penso no futuro, não tenho pressa', '🌳', '{"horizonte": "LONGO_PRAZO"}'::jsonb);

-- Pergunta 3 — Perfil de Risco
INSERT INTO quiz_pergunta (id, ordem, texto, tipo, campo_perfil, obrigatoria, ativa) VALUES
    ('a0000000-0000-4000-8000-000000000003', 3, 'Se seu investimento caísse 20% de repente, o que você faria?', 'UNICA_ESCOLHA', 'PERFIL_RISCO', TRUE, TRUE);

INSERT INTO quiz_opcao (quiz_pergunta_id, ordem, texto, emoji, mapeamento_json) VALUES
                                                                                    ('a0000000-0000-4000-8000-000000000003', 1, 'Venderia tudo imediatamente, não consigo dormir com isso', '😱', '{"perfilRisco": "CONSERVADOR"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000003', 2, 'Ficaria preocupado, mas esperaria para ver o que acontece', '😤', '{"perfilRisco": "MODERADO"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000003', 3, 'Aproveitaria para comprar mais, quedas são oportunidades', '😎', '{"perfilRisco": "ARROJADO"}'::jsonb);

-- Pergunta 4 — Valor Disponível
INSERT INTO quiz_pergunta (id, ordem, texto, tipo, campo_perfil, obrigatoria, ativa) VALUES
    ('a0000000-0000-4000-8000-000000000004', 4, 'Quanto você tem disponível para começar a investir agora?', 'UNICA_ESCOLHA', 'VALOR_DISPONIVEL', TRUE, TRUE);

INSERT INTO quiz_opcao (quiz_pergunta_id, ordem, texto, emoji, mapeamento_json) VALUES
                                                                                    ('a0000000-0000-4000-8000-000000000004', 1, 'Menos de R$ 500', NULL, '{"valorDisponivelMin": 0, "valorDisponivelMax": 500}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000004', 2, 'Entre R$ 500 e R$ 2.000', NULL, '{"valorDisponivelMin": 500, "valorDisponivelMax": 2000}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000004', 3, 'Entre R$ 2.000 e R$ 10.000', NULL, '{"valorDisponivelMin": 2000, "valorDisponivelMax": 10000}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000004', 4, 'Mais de R$ 10.000', NULL, '{"valorDisponivelMin": 10000, "valorDisponivelMax": null}'::jsonb);

-- Pergunta 5 — Tipos Aceitos (múltipla escolha)
INSERT INTO quiz_pergunta (id, ordem, texto, tipo, campo_perfil, obrigatoria, ativa) VALUES
    ('a0000000-0000-4000-8000-000000000005', 5, 'Que tipos de investimento você toparia explorar?', 'MULTIPLA_ESCOLHA', 'TIPOS_ACEITOS', TRUE, TRUE);

INSERT INTO quiz_opcao (quiz_pergunta_id, ordem, texto, emoji, mapeamento_json) VALUES
                                                                                    ('a0000000-0000-4000-8000-000000000005', 1, 'Ações de empresas da bolsa', '🏢', '{"tiposAceitos": ["ACAO"]}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000005', 2, 'Fundos Imobiliários (FIIs) — imóveis sem comprar imóvel', '🏘️', '{"tiposAceitos": ["FII"]}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000005', 3, 'ETFs — fundos que seguem índices como o Ibovespa', '📊', '{"tiposAceitos": ["ETF"]}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000005', 4, 'Não sei ainda — me mostre de tudo um pouco', '🤷', '{"tiposAceitos": ["ACAO", "FII", "ETF"]}'::jsonb);

-- Pergunta 6 — Setores Preferidos (múltipla escolha, opcional)
INSERT INTO quiz_pergunta (id, ordem, texto, tipo, campo_perfil, obrigatoria, ativa) VALUES
    ('a0000000-0000-4000-8000-000000000006', 6, 'Tem algum setor que você curte ou prefere evitar?', 'MULTIPLA_ESCOLHA', 'SETORES_PREFERIDOS', FALSE, TRUE);

INSERT INTO quiz_opcao (quiz_pergunta_id, ordem, texto, emoji, mapeamento_json) VALUES
                                                                                    ('a0000000-0000-4000-8000-000000000006', 1, 'Energia elétrica', '⚡', '{"setor": "Energia Elétrica", "preferencia": "PREFERIR"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000006', 2, 'Tecnologia', '💻', '{"setor": "Tecnologia", "preferencia": "PREFERIR"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000006', 3, 'Varejo e consumo', '🛒', '{"setor": "Varejo e Consumo", "preferencia": "PREFERIR"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000006', 4, 'Financeiro e bancos', '🏦', '{"setor": "Financeiro e Bancos", "preferencia": "PREFERIR"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000006', 5, 'Agronegócio', '🌿', '{"setor": "Agronegócio", "preferencia": "PREFERIR"}'::jsonb),
                                                                                    ('a0000000-0000-4000-8000-000000000006', 6, 'Prefiro não filtrar por setor agora', '🚫', '{}'::jsonb);