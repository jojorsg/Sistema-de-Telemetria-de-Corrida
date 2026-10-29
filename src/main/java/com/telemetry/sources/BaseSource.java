package com.telemetry.sources;

import com.telemetry.common.Constants;
import com.telemetry.proto.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseSource {
    protected final String sourceId;
    protected final String domainType;
    protected final boolean controllable;
    protected final int grpcPort;
    protected final String localIp;
    protected volatile String gatewayIp;
    protected volatile int gatewayUdpPort;
    protected final AtomicBoolean enabled = new AtomicBoolean(true);
    protected volatile int sendFrequencySeconds = 5;
    protected volatile double threshold = 100.0;
    protected Server grpcServer;
    protected final Random random = new Random();

    protected BaseSource(String sourceId, String domainType, boolean controllable, int grpcPort) {
        this.sourceId = sourceId;
        this.domainType = domainType;
        this.controllable = controllable;
        this.grpcPort = grpcPort;
        this.localIp = detectLocalIp();
    }

    public void start() throws Exception {
        System.out.println("[" + sourceId + "] Iniciando (IP=" + localIp
                + ", gRPC=" + (controllable ? grpcPort : "N/A") + ")");
        new Thread(this::multicastListenerLoop, sourceId + "-Multicast").start();
        if (controllable) startGrpcServer();
        new Thread(this::dataProducerLoop, sourceId + "-Producer").start();
    }

    private void startGrpcServer() throws IOException {
        grpcServer = ServerBuilder.forPort(grpcPort)
                .addService(new SourceControlImpl())
                .build().start();
        System.out.println("[" + sourceId + "] gRPC escutando na porta " + grpcPort);
    }

    private void multicastListenerLoop() {
        try (MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT)) {
            socket.setReuseAddress(true);
            InetAddress group = InetAddress.getByName(Constants.MULTICAST_GROUP);
            socket.joinGroup(group);
            byte[] buf = new byte[8192];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                try {
                    DiscoveryRequest req = DiscoveryRequest.parseFrom(
                            Arrays.copyOf(packet.getData(), packet.getLength()));
                    if (Constants.DISCOVERY_MSG.equals(req.getMessage())) {
                        this.gatewayIp = req.getGatewayIp();
                        this.gatewayUdpPort = req.getGatewayUdpPort();
                        System.out.println("[" + sourceId + "] Gateway descoberto em "
                                + gatewayIp + ":" + gatewayUdpPort);
                        sendDiscoveryResponse(socket, group);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[" + sourceId + "-Multicast] " + e.getMessage());
        }
    }

    private void sendDiscoveryResponse(MulticastSocket socket, InetAddress group) throws IOException {
        DiscoveryResponse resp = DiscoveryResponse.newBuilder()
                .setSourceId(sourceId)
                .setSourceType(controllable ? "controllable" : "non_controllable")
                .setDomainType(domainType)
                .setIp(localIp)
                .setUdpPort(0)
                .setGrpcPort(controllable ? grpcPort : 0)
                .setInitialState("enabled=" + enabled.get() + ";freq=" + sendFrequencySeconds
                        + ";threshold=" + threshold)
                .build();
        byte[] b = resp.toByteArray();
        socket.send(new DatagramPacket(b, b.length, group, Constants.MULTICAST_PORT));
    }

    private void dataProducerLoop() {
        while (true) {
            try {
                if (enabled.get() && gatewayIp != null) {
                    sendUdp(generateData(false, null));
                    TelemetryData ev = checkEvent();
                    if (ev != null) sendUdp(ev);
                }
                Thread.sleep(sendFrequencySeconds * 1000L);
            } catch (Exception e) {
                System.err.println("[" + sourceId + "-Producer] " + e.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void sendUdp(TelemetryData data) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] bytes = data.toByteArray();
            socket.send(new DatagramPacket(bytes, bytes.length,
                    InetAddress.getByName(gatewayIp), gatewayUdpPort));
        }
    }

    protected abstract TelemetryData generateData(boolean isEvent, String eventDesc);
    protected abstract TelemetryData checkEvent();

    protected boolean handleCustomCommand(String type, String param, String value,
                                          ControlResponse.Builder b) { return false; }

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

    protected class SourceControlImpl extends SourceControlGrpc.SourceControlImplBase {
        @Override
        public void sendCommand(ControlCommand req, StreamObserver<ControlResponse> obs) {
            String type = req.getCommandType();
            String param = req.getParameter();
            String value = req.getValue();
            ControlResponse.Builder b = ControlResponse.newBuilder();
            try {
                switch (type) {
                    case "ENABLE":
                        enabled.set(true); b.setSuccess(true).setMessage("Fonte ativada"); break;
                    case "DISABLE":
                        enabled.set(false); b.setSuccess(true).setMessage("Fonte desativada"); break;
                    case "SET_FREQUENCY":
                        sendFrequencySeconds = Integer.parseInt(value);
                        b.setSuccess(true).setMessage("Frequência: " + value + "s"); break;
                    case "SET_THRESHOLD":
                        threshold = Double.parseDouble(value);
                        b.setSuccess(true).setMessage("Limiar: " + value); break;
                    default:
                        if (!handleCustomCommand(type, param, value, b)) {
                            b.setSuccess(false).setMessage("Comando não suportado: " + type);
                        }
                }
                b.setCurrentState("enabled=" + enabled.get()
                        + ";freq=" + sendFrequencySeconds
                        + ";threshold=" + threshold);
            } catch (Exception e) {
                b.setSuccess(false).setMessage("Erro: " + e.getMessage());
            }
            obs.onNext(b.build());
            obs.onCompleted();
        }

        @Override
        public void getState(StateRequest req, StreamObserver<StateResponse> obs) {
            obs.onNext(StateResponse.newBuilder()
                    .setSourceId(sourceId)
                    .setEnabled(enabled.get())
                    .setSendFrequencySeconds(sendFrequencySeconds)
                    .setThreshold(threshold)
                    .build());
            obs.onCompleted();
        }
    }
}