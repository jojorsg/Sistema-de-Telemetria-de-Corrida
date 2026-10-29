package com.telemetry.sources;

import com.telemetry.proto.ControlResponse;
import com.telemetry.proto.TelemetryData;

public class CarroSource extends BaseSource {
    private volatile String engineMode = "RACE";
    private volatile boolean boxCalled = false;
    private volatile double fuelLimit = 100.0;
    private double currentFuel = 100.0;
    private double currentSpeed = 0;
    private double currentRpm = 0;

    public CarroSource() {
        super("CARRO-01", "carro", true, 5101);
        this.threshold = 320.0;
    }

    public static void main(String[] args) throws Exception { new CarroSource().start(); }

    @Override
    protected TelemetryData generateData(boolean isEvent, String eventDesc) {
        switch (engineMode) {
            case "RACE":
                currentSpeed = 200 + random.nextDouble() * 120;
                currentRpm   = 8000 + random.nextDouble() * 4000;
                break;
            case "SAFE":
                currentSpeed = 80 + random.nextDouble() * 40;
                currentRpm   = 4000 + random.nextDouble() * 2000;
                break;
            default: // PIT
                currentSpeed = random.nextDouble() * 60;
                currentRpm   = 2000 + random.nextDouble() * 3000;
        }
        currentFuel = Math.max(0, currentFuel - random.nextDouble() * 0.3);

        TelemetryData.Builder b = TelemetryData.newBuilder()
                .setSourceId(sourceId).setSourceType("carro")
                .setTimestamp(System.currentTimeMillis())
                .setIsEvent(isEvent);
        if (isEvent && eventDesc != null) b.setEventDescription(eventDesc);
        b.putMetrics("velocidade", currentSpeed);
        b.putMetrics("rpm", currentRpm);
        b.putMetrics("combustivel", currentFuel);
        b.putLabels("modo_motor", engineMode);
        b.putLabels("box_chamado", String.valueOf(boxCalled));
        b.putLabels("limite_combustivel", String.valueOf(fuelLimit));
        return b.build();
    }

    @Override
    protected TelemetryData checkEvent() {
        if (currentSpeed > threshold)
            return generateData(true, "Velocidade acima do limiar: " + String.format("%.1f", currentSpeed));
        if (currentFuel < fuelLimit * 0.1)
            return generateData(true, "Combustível crítico: " + String.format("%.1f", currentFuel));
        return null;
    }

    @Override
    protected boolean handleCustomCommand(String type, String param, String value,
                                          ControlResponse.Builder b) {
        switch (type) {
            case "SET_ENGINE_MODE":
                engineMode = value;
                b.setSuccess(true).setMessage("Modo do motor: " + value);
                return true;
            case "CALL_BOX":
                boxCalled = true;
                b.setSuccess(true).setMessage("Chamada ao box registrada");
                return true;
            case "SET_FUEL_LIMIT":
                fuelLimit = Double.parseDouble(value);
                b.setSuccess(true).setMessage("Limite de combustível: " + value);
                return true;
            default:
                return false;
        }
    }
}