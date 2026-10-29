package com.telemetry.gateway;

import com.telemetry.proto.TelemetryData;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CsvStore {
    private final String filename;
    private final Object lock = new Object();

    public CsvStore(String filename) throws IOException {
        this.filename = filename;
        File f = new File(filename);
        if (!f.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("timestamp,source_id,source_type,is_event,event_description,metrics,labels");
            }
        }
    }

    public void append(TelemetryData d) {
        synchronized (lock) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(filename, true))) {
                String metrics = d.getMetricsMap().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(";"));
                String labels = d.getLabelsMap().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(";"));
                pw.printf("%d,%s,%s,%b,\"%s\",\"%s\",\"%s\"%n",
                        d.getTimestamp(),
                        d.getSourceId(),
                        d.getSourceType(),
                        d.getIsEvent(),
                        d.getEventDescription().replace("\"", "'"),
                        metrics, labels);
            } catch (IOException e) {
                System.err.println("[CsvStore] " + e.getMessage());
            }
        }
    }

    public List<String[]> readAll() throws IOException {
        List<String[]> rows = new ArrayList<>();
        synchronized (lock) {
            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                String line; boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (first) { first = false; continue; }
                    rows.add(line.split(",", -1));
                }
            }
        }
        return rows;
    }
}