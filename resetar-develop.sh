#!/usr/bin/env bash
set -euo pipefail

echo "⚠️  Isso vai apagar seu 'develop' local e remoto, recriando ele igual ao 'main'."
echo "    O histórico antigo do develop remoto será perdido (force push)."
read -rp "Tem certeza que quer continuar? (s/N) " CONFIRMACAO

if [[ "$CONFIRMACAO" != "s" && "$CONFIRMACAO" != "S" ]]; then
    echo "Cancelado."
    exit 0
fi

echo "Buscando main atualizado do remoto..."
git fetch origin main

echo "Saindo de qualquer branch atual (evita erro ao deletar main/develop)..."
git checkout --detach origin/main

echo "Removendo main e develop locais, se existirem..."
git branch -D main 2>/dev/null || true
git branch -D develop 2>/dev/null || true

echo "Recriando main a partir do origin..."
git checkout -b main origin/main

echo "Recriando develop igual ao main..."
git checkout -b develop

echo "Atualizando referência local do develop remoto..."
git fetch origin develop 2>/dev/null || true

echo "Publicando develop no remoto (force-with-lease)..."
git push origin develop --force-with-lease

echo "✅ develop resetado com sucesso, igual ao main."