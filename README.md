# Sistema-de-Telemetria-de-Corrida
Sistema distribuído de monitoramento e controle (Java + Python) inspirado em telemetria de Fórmula 1. Um Gateway central coleta dados de fontes via UDP, envia comandos via gRPC, descobre fontes por multicast e atende um Cliente CLI via TCP, tudo serializado com Protobuf. Dados são persistidos em CSV e consultáveis por análises agregadas.
