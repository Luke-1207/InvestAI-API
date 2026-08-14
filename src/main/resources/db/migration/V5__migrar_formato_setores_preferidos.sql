UPDATE perfil_investidor
SET setores_preferidos = '[]'::jsonb
WHERE setores_preferidos IS NOT NULL;