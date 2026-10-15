ALTER TABLE quiz_pergunta ADD COLUMN apoio VARCHAR(500);
ALTER TABLE quiz_opcao ADD COLUMN explicacao VARCHAR(500);

UPDATE quiz_pergunta SET apoio = 'Não existe resposta certa. Seu objetivo ajuda a escolher entre investimentos que pagam renda, que crescem mais no longo prazo ou que protegem o que você já tem.' WHERE id = 'a0000000-0000-4000-8000-000000000001';
UPDATE quiz_opcao SET explicacao = 'Prioriza ativos que distribuem rendimentos com frequência, como FIIs, que costumam pagar todo mês, e ações que pagam bons dividendos.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000001' AND ordem = 1;
UPDATE quiz_opcao SET explicacao = 'Prioriza ativos com mais potencial de valorização. Eles costumam oscilar mais no caminho, por isso combinam com quem pode esperar.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000001' AND ordem = 2;
UPDATE quiz_opcao SET explicacao = 'Prioriza segurança e previsibilidade, como os títulos de renda fixa. O retorno tende a ser menor, mas as quedas também.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000001' AND ordem = 3;
UPDATE quiz_opcao SET explicacao = 'Tratamos como crescimento do patrimônio. O prazo que você informar na próxima pergunta ajuda a ajustar as sugestões.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000001' AND ordem = 4;

UPDATE quiz_pergunta SET apoio = 'É o tempo que o dinheiro pode ficar aplicado sem você precisar dele. Quanto mais tempo, mais dá para esperar as oscilações do mercado passarem.' WHERE id = 'a0000000-0000-4000-8000-000000000002';
UPDATE quiz_opcao SET explicacao = 'Para dinheiro que pode ser usado logo, o ideal são investimentos estáveis e fáceis de resgatar, como o Tesouro Selic e CDBs com liquidez diária.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000002' AND ordem = 1;
UPDATE quiz_opcao SET explicacao = 'Dá para combinar renda fixa com uma parte em renda variável, aceitando algumas oscilações no meio do caminho.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000002' AND ordem = 2;
UPDATE quiz_opcao SET explicacao = 'Com bastante tempo pela frente, a renda variável ganha espaço, porque há tempo para recuperar quedas e aproveitar o crescimento.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000002' AND ordem = 3;

UPDATE quiz_pergunta SET apoio = 'Quedas fazem parte da renda variável. A sua reação mostra quanto de oscilação você aguenta sem perder o sono, e isso define o seu perfil de risco.' WHERE id = 'a0000000-0000-4000-8000-000000000003';
UPDATE quiz_opcao SET explicacao = 'Perfil conservador: você prefere estabilidade, mesmo com um retorno menor. Vamos priorizar investimentos mais seguros.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000003' AND ordem = 1;
UPDATE quiz_opcao SET explicacao = 'Perfil moderado: você aceita algumas oscilações em troca de um retorno melhor, equilibrando segurança e crescimento.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000003' AND ordem = 2;
UPDATE quiz_opcao SET explicacao = 'Perfil arrojado: você encara as quedas como oportunidade e aceita mais risco em busca de retornos maiores no longo prazo.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000003' AND ordem = 3;

UPDATE quiz_pergunta SET apoio = 'Serve só para ajustar as sugestões ao seu bolso, por exemplo mostrando títulos com investimento mínimo que caiba nesse valor. Não precisa ser exato.' WHERE id = 'a0000000-0000-4000-8000-000000000004';
UPDATE quiz_opcao SET explicacao = 'Dá para começar com pouco. Vários títulos do Tesouro Direto e cotas de FIIs custam menos do que isso.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000004' AND ordem = 1;
UPDATE quiz_opcao SET explicacao = 'Já dá para dividir o valor entre alguns investimentos diferentes, o que ajuda a diminuir o risco.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000004' AND ordem = 2;
UPDATE quiz_opcao SET explicacao = 'Permite montar uma carteira variada, combinando renda fixa e renda variável.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000004' AND ordem = 3;
UPDATE quiz_opcao SET explicacao = 'Abre espaço para praticamente todos os títulos e ativos da plataforma, inclusive os de investimento mínimo mais alto.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000004' AND ordem = 4;

UPDATE quiz_pergunta SET texto = 'Na renda variável, quais tipos de investimento você quer ver nas suas sugestões?' WHERE id = 'a0000000-0000-4000-8000-000000000005';
UPDATE quiz_pergunta SET apoio = 'Marque um ou mais. Os tipos que você não marcar ficam de fora das sugestões de renda variável. A renda fixa (Tesouro Direto, CDB, LCI e LCA) é sugerida para todos os perfis.' WHERE id = 'a0000000-0000-4000-8000-000000000005';
UPDATE quiz_opcao SET texto = 'Ações' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000005' AND ordem = 1;
UPDATE quiz_opcao SET texto = 'Fundos Imobiliários (FIIs)' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000005' AND ordem = 2;
UPDATE quiz_opcao SET texto = 'ETFs' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000005' AND ordem = 3;
UPDATE quiz_opcao SET texto = 'Ainda não sei, quero ver todos' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000005' AND ordem = 4;
UPDATE quiz_opcao SET explicacao = 'Uma ação é um pedacinho de uma empresa. Você ganha se a empresa se valorizar e ainda pode receber parte do lucro em dividendos.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000005' AND ordem = 1;
UPDATE quiz_opcao SET explicacao = 'Um FII junta o dinheiro de muitas pessoas para investir em imóveis, como shoppings e galpões. Os aluguéis viram rendimentos, em geral pagos todo mês.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000005' AND ordem = 2;
UPDATE quiz_opcao SET explicacao = 'Um ETF é um fundo negociado na bolsa que segue um índice, como o Ibovespa. Com uma única cota você investe em várias empresas de uma vez.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000005' AND ordem = 3;
UPDATE quiz_opcao SET explicacao = 'Sem problema. Vamos sugerir os três tipos, e você pode refazer o quiz quando quiser.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000005' AND ordem = 4;

UPDATE quiz_pergunta SET texto = 'Tem algum setor que você gostaria de ver com mais destaque nas sugestões?' WHERE id = 'a0000000-0000-4000-8000-000000000006';
UPDATE quiz_pergunta SET apoio = 'Opcional. Os setores que você marcar ganham prioridade nas sugestões de renda variável, sem esconder os outros. Na próxima pergunta você pode escolher setores para evitar.' WHERE id = 'a0000000-0000-4000-8000-000000000006';
UPDATE quiz_opcao SET explicacao = 'Empresas que geram, transmitem e distribuem energia. Costumam ter receita estável e pagar bons dividendos.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000006' AND ordem = 1;
UPDATE quiz_opcao SET explicacao = 'Empresas de software, internet e serviços digitais. Podem crescer rápido, mas costumam oscilar mais.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000006' AND ordem = 2;
UPDATE quiz_opcao SET explicacao = 'Lojas e marcas que vendem direto para o consumidor. Os resultados dependem muito de como vão a economia e o emprego.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000006' AND ordem = 3;
UPDATE quiz_opcao SET explicacao = 'Bancos, seguradoras e empresas de pagamento. Em geral são empresas grandes e lucrativas, que pagam dividendos com frequência.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000006' AND ordem = 4;
UPDATE quiz_opcao SET explicacao = 'Empresas ligadas à produção e venda de alimentos e matérias-primas agrícolas. São influenciadas pelo clima e pelos preços no mercado internacional.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000006' AND ordem = 5;
UPDATE quiz_opcao SET texto = 'Nenhum em especial' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000006' AND ordem = 6;
UPDATE quiz_opcao SET explicacao = 'Todos os setores vão ter o mesmo peso nas suas sugestões.' WHERE quiz_pergunta_id = 'a0000000-0000-4000-8000-000000000006' AND ordem = 6;

INSERT INTO quiz_pergunta (id, ordem, texto, apoio, tipo, campo_perfil, obrigatoria, ativa) VALUES
    ('a0000000-0000-4000-8000-000000000007', 7, 'E tem algum setor que você prefere evitar?', 'Opcional. Ações e FIIs desses setores perdem prioridade nas suas sugestões. Um setor que você marcou para ver com destaque não pode ser evitado ao mesmo tempo.', 'MULTIPLA_ESCOLHA', 'SETORES_PREFERIDOS', FALSE, TRUE);

INSERT INTO quiz_opcao (quiz_pergunta_id, ordem, texto, emoji, explicacao, mapeamento_json) VALUES
    ('a0000000-0000-4000-8000-000000000007', 1, 'Energia elétrica', '⚡', 'Empresas que geram, transmitem e distribuem energia. Costumam ter receita estável e pagar bons dividendos.', '{"setor": "Energia Elétrica", "preferencia": "EVITAR"}'::jsonb),
    ('a0000000-0000-4000-8000-000000000007', 2, 'Tecnologia', '💻', 'Empresas de software, internet e serviços digitais. Podem crescer rápido, mas costumam oscilar mais.', '{"setor": "Tecnologia", "preferencia": "EVITAR"}'::jsonb),
    ('a0000000-0000-4000-8000-000000000007', 3, 'Varejo e consumo', '🛒', 'Lojas e marcas que vendem direto para o consumidor. Os resultados dependem muito de como vão a economia e o emprego.', '{"setor": "Varejo e Consumo", "preferencia": "EVITAR"}'::jsonb),
    ('a0000000-0000-4000-8000-000000000007', 4, 'Financeiro e bancos', '🏦', 'Bancos, seguradoras e empresas de pagamento. Em geral são empresas grandes e lucrativas, que pagam dividendos com frequência.', '{"setor": "Financeiro e Bancos", "preferencia": "EVITAR"}'::jsonb),
    ('a0000000-0000-4000-8000-000000000007', 5, 'Agronegócio', '🌿', 'Empresas ligadas à produção e venda de alimentos e matérias-primas agrícolas. São influenciadas pelo clima e pelos preços no mercado internacional.', '{"setor": "Agronegócio", "preferencia": "EVITAR"}'::jsonb),
    ('a0000000-0000-4000-8000-000000000007', 6, 'Não quero evitar nenhum', '✅', 'Nenhum setor perde prioridade nas suas sugestões.', '{}'::jsonb);
