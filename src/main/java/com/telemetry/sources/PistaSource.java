package com.telemetry.sources;

import com.telemetry.proto.TelemetryData;

public class PistaSource extends BaseSource {
    public PistaSource() { super("PISTA-01", "pista", false, 0); }

    public static void main(String[] args) throws Exception { new PistaSource().start(); }

    @Override
    protected TelemetryData generateData(boolean isEvent, String eventDesc) {
        double tempAsfalto = 30 + random.nextDouble() * 20;
        double chuva = random.nextDouble() < 0.2 ? random.nextDouble() * 10 : 0;
        TelemetryData.Builder b = TelemetryData.newBuilder()
                .setSourceId(sourceId).setSourceType("pista")
                .setTimestamp(System.currentTimeMillis())
                .setIsEvent(isEvent);
        if (isEvent && eventDesc != null) b.setEventDescription(eventDesc);
        b.putMetrics("temperatura_asfalto", tempAsfalto);
        b.putMetrics("chuva_mm", chuva);
        return b.build();
    }

    @Override
    protected TelemetryData checkEvent() {
        if (random.nextDouble() < 0.05)
            return generateData(true, "Chuva detectada na pista");
        return null;
    }
}