package com.telemetry.gateway;

import com.google.protobuf.InvalidProtocolBufferException;
import com.telemetry.common.Constants;
import com.telemetry.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Gateway {
    private final Map<String, SourceInfo> sources = new ConcurrentHashMap<>();
    private final Map<String, ManagedChannel> grpcChannels = new ConcurrentHashMap<>();
    private final List<TelemetryData> dataBuffer = Collections.synchronizedList(new ArrayList<>());
    private final CsvStore csvStore;
    private volatile boolean running = true;
    private final String localIp;

    public Gateway() throws IOException {
        this.csvStore = new CsvStore(Constants.CSV_FILE);
        this.localIp = detectLocalIp();
    }

    public static void main(String[] args) throws Exception {
        new Gateway().start();
    }

    public void start() throws Exception {
        System.out.println("[Gateway] Iniciando. IP local: " + localIp);
        new Thread(this::discoveryLoop, "Discovery").start();
        new Thread(this::multicastListenerLoop, "MulticastListener").start();
        new Thread(this::udpServerLoop, "UDPServer").start();
        new Thread(this::tcpServerLoop, "TCPServer").start();
        System.out.println("[Gateway] Pronto! TCP=" + Constants.GATEWAY_TCP_PORT
                + " UDP=" + Constants.GATEWAY_UDP_PORT
                + " Multicast=" + Constants.MULTICAST_GROUP + ":" + Constants.MULTICAST_PORT);
    }

    private String detectLocalIp() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                NetworkInterface ni = ifs.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    // ---------- Descoberta: envio periódico de pedido multicast ----------
    private void discoveryLoop() {
        try (MulticastSocket socket = new MulticastSocket()) {
            socket.setTimeToLive(1);
            InetAddress group = InetAddress.getByName(Constants.MULTICAST_GROUP);
            DiscoveryRequest req = DiscoveryRequest.newBuilder()
                    .setGatewayId("GATEWAY-1")
                    .setGatewayIp(localIp)
                    .setGatewayUdpPort(Constants.GATEWAY_UDP_PORT)
                    .setMessage(Constants.DISCOVERY_MSG)
                    .build();
            byte[] buf = req.toByteArray();
            while (running) {
                socket.send(new DatagramPacket(buf, buf.length, group, Constants.MULTICAST_PORT));
                System.out.println("[Discovery] Pedido de descoberta enviado.");
                Thread.sleep(10_000);
            }
        } catch (Exception e) {
            System.err.println("[Discovery] " + e.getMessage());
        }
    }

    // ---------- Escuta de respostas multicast ----------
    private void multicastListenerLoop() {
        try (MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT)) {
            socket.setReuseAddress(true);
            InetAddress group = InetAddress.getByName(Constants.MULTICAST_GROUP);
            socket.joinGroup(group);
            byte[] buf = new byte[8192];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                try {
                    DiscoveryResponse resp = DiscoveryResponse.parseFrom(
                            Arrays.copyOf(packet.getData(), packet.getLength()));
                    // Ignora o eco do próprio multicast (DiscoveryRequest parseada como Response)
                    if (resp.getSourceId().isEmpty() || resp.getDomainType().isEmpty()) continue;
                    registerSource(resp);
                } catch (InvalidProtocolBufferException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[MulticastListener] " + e.getMessage());
        }
    }

    private void registerSource(DiscoveryResponse resp) {
        SourceInfo existing = sources.get(resp.getSourceId());
        // Mesmo ID e mesma porta gRPC: já registrada, ignora (evita canal duplicado)
        if (existing != null && existing.grpcPort == resp.getGrpcPort()) return;

        // Se existia com porta diferente, descarta canal antigo
        if (existing != null) {
            ManagedChannel old = grpcChannels.remove(resp.getSourceId());
            if (old != null) old.shutdownNow();
            sources.remove(resp.getSourceId());
        }

        SourceInfo info = new SourceInfo();
        info.sourceId     = resp.getSourceId();
        info.sourceType   = resp.getSourceType();
        info.domainType   = resp.getDomainType();
        info.ip           = resp.getIp();
        info.udpPort      = resp.getUdpPort();
        info.grpcPort     = resp.getGrpcPort();
        info.initialState = resp.getInitialState();
        sources.put(info.sourceId, info);
        System.out.println("[Gateway] Fonte registrada: " + info.sourceId
                + " | domínio=" + info.domainType
                + " | controlável=" + info.isControllable());
        if (info.isControllable()) {
            ManagedChannel ch = ManagedChannelBuilder
                    .forAddress(info.ip, info.grpcPort)
                    .usePlaintext().build();
            grpcChannels.put(info.sourceId, ch);
        }
    }

    // ---------- Servidor UDP para dados de telemetria ----------
    private void udpServerLoop() {
        try (DatagramSocket socket = new DatagramSocket(Constants.GATEWAY_UDP_PORT)) {
            byte[] buf = new byte[65536];
            System.out.println("[UDP] Escutando na porta " + Constants.GATEWAY_UDP_PORT);
            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                try {
                    TelemetryData data = TelemetryData.parseFrom(
                            Arrays.copyOf(packet.getData(), packet.getLength()));
                    dataBuffer.add(data);
                    csvStore.append(data);
                    System.out.println("[UDP] " + data.getSourceId() + " -> "
                            + data.getMetricsMap()
                            + (data.getIsEvent() ? " [EVENTO: " + data.getEventDescription() + "]" : ""));
                    if (dataBuffer.size() > 10000) {
                        synchronized (dataBuffer) {
                            dataBuffer.subList(0, 1000).clear();
                        }
                    }
                } catch (InvalidProtocolBufferException e) {
                    System.err.println("[UDP] Pacote inválido");
                }
            }
        } catch (Exception e) {
            System.err.println("[UDPServer] " + e.getMessage());
        }
    }

    // ---------- Servidor TCP para o Cliente ----------
    private void tcpServerLoop() {
        try (ServerSocket server = new ServerSocket(Constants.GATEWAY_TCP_PORT)) {
            System.out.println("[TCP] Escutando na porta " + Constants.GATEWAY_TCP_PORT);
            while (running) {
                Socket client = server.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } catch (IOException e) {
            System.err.println("[TCP] " + e.getMessage());
        }
    }

    private void handleClient(Socket client) {
        try (DataInputStream in = new DataInputStream(client.getInputStream());
             DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
            while (true) {
                int len = in.readInt();
                if (len <= 0) break;
                byte[] buf = new byte[len];
                in.readFully(buf);
                ClientRequest req = ClientRequest.parseFrom(buf);
                ClientResponse resp = processRequest(req);
                byte[] respBytes = resp.toByteArray();
                out.writeInt(respBytes.length);
                out.write(respBytes);
                out.flush();
            }
        } catch (Exception ignored) {}
    }

    private ClientResponse processRequest(ClientRequest req) {
        ClientResponse.Builder b = ClientResponse.newBuilder().setRequestId(req.getRequestId());
        try {
            switch (req.getAction()) {
                case "LIST_SOURCES": {
                    StringBuilder sb = new StringBuilder();
                    if (sources.isEmpty()) sb.append("(Nenhuma fonte descoberta ainda)");
                    for (SourceInfo s : sources.values()) {
                        sb.append(s.sourceId)
                          .append(" | domínio=").append(s.domainType)
                          .append(" | controlável=").append(s.isControllable())
                          .append(" | ip=").append(s.ip)
                          .append(" | gRPC=").append(s.grpcPort)
                          .append(" | estado=").append(s.initialState).append("\n");
                    }
                    b.setSuccess(true).setMessage("Fontes conectadas").setData(sb.toString());
                    break;
                }

                case "SEND_COMMAND": {
                    String[] p = req.getPayload().split("\\|", -1);
                    if (p.length < 2) {
                        b.setSuccess(false).setMessage("Payload de comando malformado.");
                        break;
                    }
                    String sid = p[0], cmd = p[1];
                    String param = p.length > 2 ? p[2] : "";
                    String value = p.length > 3 ? p[3] : "";

                    SourceInfo s = sources.get(sid);
                    if (s == null) {
                        b.setSuccess(false).setMessage("Fonte '" + sid + "' não encontrada.");
                        break;
                    }
                    if (!s.isControllable()) {
                        b.setSuccess(false).setMessage("Fonte '" + sid + "' não é controlável.");
                        break;
                    }

                    ManagedChannel channel = grpcChannels.get(sid);
                    if (channel == null || channel.isShutdown() || channel.isTerminated()) {
                        b.setSuccess(false).setMessage("Fonte '" + sid + "' está offline.");
                        break;
                    }

                    try {
                        SourceControlGrpc.SourceControlBlockingStub stub =
                                SourceControlGrpc.newBlockingStub(channel);
                        ControlCommand cc = ControlCommand.newBuilder()
                                .setCommandId(UUID.randomUUID().toString())
                                .setCommandType(cmd).setParameter(param).setValue(value).build();
                        ControlResponse cr = stub.sendCommand(cc);
                        b.setSuccess(cr.getSuccess())
                         .setMessage(cr.getMessage())
                         .setData(cr.getCurrentState());
                    } catch (StatusRuntimeException e) {
                        // Fonte caiu ou não respondeu: mensagem amigável + limpeza do registro
                        b.setSuccess(false).setMessage("Fonte '" + sid
                                + "' está offline ou não respondeu (" + e.getStatus().getCode() + ").");
                        sources.remove(sid);
                        ManagedChannel ch = grpcChannels.remove(sid);
                        if (ch != null) ch.shutdownNow();
                        System.out.println("[Gateway] Fonte removida (offline): " + sid);
                    }
                    break;
                }

                case "ANALYTICS": {
                    String[] p = req.getPayload().split("\\|", -1);
                    if (p.length < 4) {
                        b.setSuccess(false).setMessage("Payload de analytics malformado.");
                        break;
                    }
                    String kind = p[0], sid = p[1], metric = p[2];
                    long windowMs;
                    try {
                        windowMs = Long.parseLong(p[3]);
                    } catch (NumberFormatException e) {
                        b.setSuccess(false).setMessage("Janela inválida: '" + p[3] + "'.");
                        break;
                    }
                    if (windowMs <= 0) {
                        b.setSuccess(false).setMessage("A janela deve ser maior que zero.");
                        break;
                    }

                    // A métrica existe em algum dado? (protegido contra concorrência com a thread UDP)
                    boolean hasMetric;
                    Set<String> available = new TreeSet<>();
                    synchronized (dataBuffer) {
                        hasMetric = dataBuffer.stream()
                                .anyMatch(d -> d.getMetricsMap().containsKey(metric));
                        if (!hasMetric) {
                            for (TelemetryData d : dataBuffer) {
                                available.addAll(d.getMetricsMap().keySet());
                            }
                        }
                    }
                    if (!hasMetric) {
                        b.setSuccess(false).setMessage("Métrica '" + metric + "' não encontrada. "
                                + "Disponíveis: " + (available.isEmpty() ? "(sem dados)" : available));
                        break;
                    }

                    String result;
                    switch (kind) {
                        case "AVG":
                            synchronized (dataBuffer) {
                                result = "Média de '" + metric + "' = "
                                        + String.format(Locale.ROOT, "%.3f",
                                            Analytics.average(dataBuffer, sid, metric, windowMs));
                            }
                            break;
                        case "STDDEV":
                            synchronized (dataBuffer) {
                                result = "Desvio padrão de '" + metric + "' = "
                                        + String.format(Locale.ROOT, "%.3f",
                                            Analytics.stdDev(dataBuffer, sid, metric, windowMs));
                            }
                            break;
                        case "MAXVAR":
                            synchronized (dataBuffer) {
                                result = "Fonte com maior variação em '" + metric + "': "
                                        + Analytics.sourceWithLargestVariation(dataBuffer, metric, windowMs);
                            }
                            break;
                        default:
                            b.setSuccess(false).setMessage("Tipo de consulta desconhecido: " + kind);
                            return b.build();
                    }
                    b.setSuccess(true).setMessage("Consulta realizada").setData(result);
                    break;
                }

                case "HISTORY": {
                    String[] p = req.getPayload().split("\\|", -1);
                    if (p.length < 2) {
                        b.setSuccess(false).setMessage("Payload de histórico malformado.");
                        break;
                    }
                    String metric = p[0];
                    long windowMs;
                    try {
                        windowMs = Long.parseLong(p[1]);
                    } catch (NumberFormatException e) {
                        b.setSuccess(false).setMessage("Janela inválida: '" + p[1] + "'.");
                        break;
                    }
                    if (windowMs <= 0) {
                        b.setSuccess(false).setMessage("A janela deve ser maior que zero.");
                        break;
                    }

                    long cutoff = System.currentTimeMillis() - windowMs;
                    StringBuilder sb = new StringBuilder();
                    synchronized (dataBuffer) {
                        for (TelemetryData d : dataBuffer) {
                            if (d.getTimestamp() >= cutoff && d.getMetricsMap().containsKey(metric)) {
                                sb.append(new Date(d.getTimestamp())).append(" | ")
                                  .append(d.getSourceId()).append(" | ")
                                  .append(metric).append("=")
                                  .append(String.format(Locale.ROOT, "%.3f",
                                          d.getMetricsMap().get(metric)))
                                  .append("\n");
                            }
                        }
                    }
                    if (sb.length() == 0) sb.append("(Sem dados no período para a métrica '")
                            .append(metric).append("')");
                    b.setSuccess(true).setMessage("Histórico").setData(sb.toString());
                    break;
                }

                default:
                    b.setSuccess(false).setMessage("Ação desconhecida: " + req.getAction());
            }
        } catch (Exception e) {
            b.setSuccess(false).setMessage("Erro: " + e.getMessage());
        }
        return b.build();
    }
}