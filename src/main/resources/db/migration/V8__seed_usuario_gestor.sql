INSERT INTO usuarios (id, nome, email, senha, role, ativo)
VALUES (
           gen_random_uuid(),
           'Lucas Gestor',
           'gestor@gmail.com',
           '$2b$10$aLOjN8V7ROLVp/BQ96fLguDKkUkczfvooClPnQh7A5pR7Uy2Mi/LS',
           'GESTOR',
           true
       )
    ON CONFLICT (email) DO NOTHING;