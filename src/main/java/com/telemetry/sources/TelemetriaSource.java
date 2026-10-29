package com.telemetry.sources;

import com.telemetry.proto.TelemetryData;

public class TelemetriaSource extends BaseSource {
    public TelemetriaSource() {
        super("TELEMETRIA-01", "telemetria_carro", false, 0);
        this.sendFrequencySeconds = 2;
    }

    public static void main(String[] args) throws Exception { new TelemetriaSource().start(); }

    @Override
    protected TelemetryData generateData(boolean isEvent, String eventDesc) {
        double vel = 150 + random.nextDouble() * 150;
        double rpm = 7000 + random.nextDouble() * 5000;
        double tempPneu = 80 + random.nextDouble() * 40;

        TelemetryData.Builder b = TelemetryData.newBuilder()
                .setSourceId(sourceId).setSourceType("telemetria_carro")
                .setTimestamp(System.currentTimeMillis())
                .setIsEvent(isEvent);
        if (isEvent && eventDesc != null) b.setEventDescription(eventDesc);
        b.putMetrics("velocidade", vel);
        b.putMetrics("rpm", rpm);
        b.putMetrics("temperatura_pneu", tempPneu);
        return b.build();
    }

    @Override
    protected TelemetryData checkEvent() {
        double temp = 80 + random.nextDouble() * 40;
        if (temp > 110)
            return generateData(true, "Temperatura do pneu crítica: " + String.format("%.1f", temp));
        return null;
    }
}