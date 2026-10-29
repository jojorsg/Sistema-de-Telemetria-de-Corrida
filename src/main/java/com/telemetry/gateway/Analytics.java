package com.telemetry.gateway;

import com.telemetry.proto.TelemetryData;

import java.util.*;

public class Analytics {

    public static double average(List<TelemetryData> data, String sourceId, String metric, long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        double sum = 0; int count = 0;
        for (TelemetryData d : data) {
            if (d.getTimestamp() < cutoff) continue;
            if (!sourceId.isEmpty() && !d.getSourceId().equals(sourceId)) continue;
            if (d.getMetricsMap().containsKey(metric)) {
                sum += d.getMetricsMap().get(metric); count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    public static double stdDev(List<TelemetryData> data, String sourceId, String metric, long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        List<Double> values = new ArrayList<>();
        for (TelemetryData d : data) {
            if (d.getTimestamp() < cutoff) continue;
            if (!sourceId.isEmpty() && !d.getSourceId().equals(sourceId)) continue;
            if (d.getMetricsMap().containsKey(metric)) values.add(d.getMetricsMap().get(metric));
        }
        if (values.size() < 2) return 0;
        double mean = values.stream().mapToDouble(v -> v).average().orElse(0);
        double var = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / (values.size() - 1);
        return Math.sqrt(var);
    }

    public static String sourceWithLargestVariation(List<TelemetryData> data, String metric, long windowMs) {
        long cutoff = System.currentTimeMillis() - windowMs;
        Map<String, List<Double>> bySource = new HashMap<>();
        for (TelemetryData d : data) {
            if (d.getTimestamp() < cutoff) continue;
            if (d.getMetricsMap().containsKey(metric)) {
                bySource.computeIfAbsent(d.getSourceId(), k -> new ArrayList<>())
                        .add(d.getMetricsMap().get(metric));
            }
        }
        String best = null; double bestVar = -1;
        for (Map.Entry<String, List<Double>> e : bySource.entrySet()) {
            if (e.getValue().size() < 2) continue;
            double mean = e.getValue().stream().mapToDouble(v -> v).average().orElse(0);
            double var = e.getValue().stream().mapToDouble(v -> (v - mean) * (v - mean)).sum()
                    / (e.getValue().size() - 1);
            if (var > bestVar) { bestVar = var; best = e.getKey(); }
        }
        return best == null ? "N/A" :
                best + " (variância=" + String.format("%.3f", bestVar) + ")";
    }
}