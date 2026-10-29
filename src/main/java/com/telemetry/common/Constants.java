package com.telemetry.common;

public final class Constants {
    private Constants() {}

    public static final String MULTICAST_GROUP  = "230.0.0.1";
    public static final int    MULTICAST_PORT   = 5002;
    public static final int    GATEWAY_TCP_PORT = 5000;
    public static final int    GATEWAY_UDP_PORT = 5001;
    public static final String CSV_FILE         = "telemetry_data.csv";
    public static final String DISCOVERY_MSG    = "TELEMETRY_DISCOVERY";
}