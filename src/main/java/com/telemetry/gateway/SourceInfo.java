package com.telemetry.gateway;

public class SourceInfo {
    public String sourceId;
    public String sourceType;
    public String domainType;
    public String ip;
    public int    udpPort;
    public int    grpcPort;
    public String initialState;

    public boolean isControllable() { return grpcPort > 0; }
}