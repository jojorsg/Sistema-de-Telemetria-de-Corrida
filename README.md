# Sistema de Telemetria de Corrida

Sistema distribuído de monitoramento e controle (Java + Python) inspirado em telemetria de Fórmula 1. Um **Gateway** central coleta dados de fontes via **UDP**, envia comandos via **gRPC**, descobre fontes por **multicast** e atende um **Cliente CLI** via **TCP**, tudo serializado com **Protobuf**. Dados são persistidos em CSV e consultáveis por análises agregadas.

---

**Portas:** Gateway `TCP 5000` / `UDP 5001` · Multicast `230.0.0.1:5002` · Carro gRPC `5101` · Sensor Ajustável gRPC `5102`.

---

## 📂 Estrutura

```
telemetry-system/
├── pom.xml
├── telemetry_data.csv                 # gerado em runtime
├── src/main/proto/telemetry.proto
├── src/main/java/com/telemetry/
│   ├── common/Constants.java
│   ├── gateway/                       Gateway · SourceInfo · CsvStore · Analytics
│   ├── client/                        Client
│   └── sources/                       BaseSource · CarroSource · PistaSource · TelemetriaSource
└── python/
    ├── source_sensor_ajustavel.py
    ├── generate_stubs.sh
    └── .venv/                         # criado pelo script
```

---

## 📖 Arquivos Principais

| Arquivo | Responsabilidade |
|---|---|
| **`telemetry.proto`** | Contrato Protobuf/gRPC (Discovery, TelemetryData, ControlCommand, ClientRequest/Response, serviço `SourceControl`) |
| **`Constants.java`** | Portas, grupo multicast, nome do CSV |
| **`SourceInfo.java`** | Metadados de fonte (ID, tipo, IP, porta gRPC, estado) |
| **`CsvStore.java`** | `append(TelemetryData)` + `readAll()` em CSV |
| **`Analytics.java`** | `average`, `stdDev`, `sourceWithLargestVariation` |
| **`Gateway.java`** | 4 threads (descoberta, multicast, UDP, TCP). Registra fontes, persiste, atende cliente. Trata fontes offline removendo-as |
| **`Client.java`** | CLI interativo: `listSources`, `sendCommand`, `analytics`, `history`. Normaliza case e valida entradas |
| **`BaseSource.java`** | Classe base: multicast listener, servidor gRPC, loop de envio UDP, valida `SET_FREQUENCY` (1–300) e `SET_THRESHOLD` (numérico) |
| **`CarroSource.java`** | Fonte **controlável** `CARRO-01`. Métricas: velocidade, rpm, combustível. Eventos: velocidade > 320, combustível crítico. Comandos: `SET_ENGINE_MODE` (RACE/SAFE/PIT), `CALL_BOX`, `SET_FUEL_LIMIT` |
| **`PistaSource.java`** | Fonte **não controlável** `PISTA-01`. Métricas: temperatura_asfalto, chuva_mm |
| **`TelemetriaSource.java`** | Fonte **não controlável** `TELEMETRIA-01`. Métricas: velocidade, rpm, temperatura_pneu |
| **`source_sensor_ajustavel.py`** | Fonte **controlável** Python `SENSOR-AJUSTAVEL-01`. Métrica: vibracao |
| **`generate_stubs.sh`** | Cria venv, instala deps, copia `.proto`, gera stubs Python |

---

## 🛠️ Pré-requisitos

- **Java 17+**, **Maven 3.8+**, **Python 3.9+** com `python3-venv`
- `sudo apt install -y python3-venv python3-full` (Debian/Ubuntu)

---

## 🔨 Compilação

```bash
cd ~/telemetry-system
mvn clean package -DskipTests     # compila Java + gera stubs Protobuf
./python/generate_stubs.sh        # cria venv + gera stubs Python
```

---

## ▶️ Execução (6 terminais)

| # | Comando |
|---|---|
| **1 — Gateway** | `mvn compile exec:java -Pgateway` |
| **2 — Carro (A)** | `mvn compile exec:java -Pcarro` |
| **3 — Pista (C)** | `mvn compile exec:java -Ppista` |
| **4 — Telemetria (D)** | `mvn compile exec:java -Ptelemetria` |
| **5 — Sensor (B, Python)** | `cd python && source .venv/bin/activate && python3 source_sensor_ajustavel.py` |
| **6 — Cliente** | `mvn compile exec:java -Pcliente` |

O Gateway imprime `[Gateway] Pronto!` e registra cada fonte conforme as descobertas chegam.

---

## 💬 Uso do Cliente

```
--- MENU ---
1 - Listar fontes conectadas
2 - Enviar comando para fonte controlável
3 - Consultas analíticas agregadas
4 - Consultar histórico (CSV)
0 - Sair
```

**Dica global:** `/back` (ou `/cancel`, `/menu`, `..`, `voltar`, `cancelar`) em qualquer prompt volta ao menu.

**Exemplo — mudar modo do motor:**
```
> 2
ID: carro-01          → [INFO] Fonte interpretada como: CARRO-01
Comando: set_engine_mode
Valor: safe           → [OK] Modo do motor: SAFE
```

**Exemplo — média:**
```
> 3 → 1
ID: (ENTER = todas)
Métrica: velocidade
Janela: 5             → >>> Média de 'velocidade' = 245.782
```

---

## 🎛️ Comandos e Métricas

| Comando | Valor | Aplicação |
|---|---|---|
| `ENABLE` / `DISABLE` | — | Todas controláveis |
| `SET_FREQUENCY` | int 1–300 | Todas controláveis |
| `SET_THRESHOLD` | double | Todas controláveis |
| `SET_ENGINE_MODE` | RACE/SAFE/PIT | Carro |
| `CALL_BOX` | — | Carro |
| `SET_FUEL_LIMIT` | double | Carro |

**Métricas:** `velocidade`, `rpm`, `combustivel`, `temperatura_asfalto`, `chuva_mm`, `temperatura_pneu`, `vibracao`.

---

## 📊 CSV

`telemetry_data.csv` — uma linha por leitura:

```
timestamp,source_id,source_type,is_event,event_description,metrics,labels
1762357835123,CARRO-01,carro,false,"","velocidade=250.3;rpm=10500.0;...","modo_motor=RACE;..."
```

---


## ✅ Requisitos Atendidos

- Protobuf em **todos** os canais (TCP, gRPC, UDP, multicast)
- **4 fontes**, sendo 2 controláveis (Java + Python) e 2 não controláveis (somente Java)
- Envio por evento em todas as fontes (limiar cruzado)
- **3 consultas analíticas** (média, desvio padrão, maior variação) + histórico CSV
- Persistência em CSV com consultas
- Tratamento de falhas: entradas inválidas, comandos não suportados, fontes offline, valores fora de faixa