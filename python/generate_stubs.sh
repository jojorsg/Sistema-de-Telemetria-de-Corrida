#!/usr/bin/env bash
set -e

# Vai para a pasta onde este script está (python/)
cd "$(dirname "$0")"

# 1. Cria o venv se não existir
if [ ! -d ".venv" ]; then
    echo "==> Criando ambiente virtual (.venv)..."
    python3 -m venv .venv
else
    echo "==> Ambiente virtual já existe, reutilizando."
fi

# 2. Ativa o venv
source .venv/bin/activate

# 3. Instala/atualiza dependências
echo "==> Instalando dependências Python..."
pip install --upgrade pip >/dev/null
pip install grpcio grpcio-tools protobuf >/dev/null

# 4. Copia o .proto para a pasta local
echo "==> Copiando telemetry.proto..."
cp ../src/main/proto/telemetry.proto .

# 5. Gera os stubs
echo "==> Gerando stubs Protobuf/gRPC..."
python3 -m grpc_tools.protoc -I. \
    --python_out=. \
    --grpc_python_out=. \
    telemetry.proto

echo ""
echo "Stubs gerados com sucesso:"
ls -la telemetry_pb2*.py
echo ""
echo "Para rodar a Fonte B:"
echo "   cd python && source .venv/bin/activate && python3 source_sensor_ajustavel.py"