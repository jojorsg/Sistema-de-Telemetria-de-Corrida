package com.telemetry.client;

import com.telemetry.common.Constants;
import com.telemetry.proto.ClientRequest;
import com.telemetry.proto.ClientResponse;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;

public class Client {
    private DataInputStream in;
    private DataOutputStream out;

    /** Exceção interna usada para abortar qualquer prompt e voltar ao menu. */
    private static class BackToMenu extends RuntimeException {
        BackToMenu() { super(null, null, false, false); }
    }

    public static void main(String[] args) {
        try {
            new Client().run();
        } catch (Exception e) {
            System.err.println("[ERRO FATAL] " + e.getMessage());
        }
    }

    public void run() {
        Socket socket = null;
        try {
            socket = new Socket("localhost", Constants.GATEWAY_TCP_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("[ERRO] Não foi possível conectar ao Gateway em localhost:"
                    + Constants.GATEWAY_TCP_PORT + ".");
            System.out.println("       Verifique se o Gateway está em execução e tente novamente.");
            return;
        }

        System.out.println("=== Cliente de Telemetria de Corrida ===");
        System.out.println("Dica: digite /back em qualquer prompt para voltar ao menu.");
        Scanner sc = new Scanner(System.in);

        try {
            while (true) {
                printMenu();
                String opt = sc.nextLine().trim();
                try {
                    switch (opt) {
                        case "1": listSources(); break;
                        case "2": sendCommand(sc); break;
                        case "3": analytics(sc); break;
                        case "4": history(sc); break;
                        case "0":
                            System.out.println("Encerrando cliente...");
                            return;
                        default:
                            System.out.println("[AVISO] Opção inválida. Digite um número entre 0 e 4.");
                    }
                } catch (BackToMenu e) {
                    System.out.println("[INFO] Operação cancelada. Voltando ao menu.");
                } catch (IOException e) {
                    System.out.println("[ERRO] Falha de comunicação com o Gateway: " + e.getMessage());
                }
            }
        } finally {
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        }
    }

    private void printMenu() {
        System.out.println("\n--- MENU ---");
        System.out.println("1 - Listar fontes conectadas");
        System.out.println("2 - Enviar comando para fonte controlável");
        System.out.println("3 - Consultas analíticas agregadas");
        System.out.println("4 - Consultar histórico (CSV)");
        System.out.println("0 - Sair");
        System.out.print("> ");
    }

    private ClientResponse send(String action, String payload) throws IOException {
        ClientRequest req = ClientRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setAction(action).setPayload(payload).build();
        byte[] bytes = req.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
        int len = in.readInt();
        byte[] respBytes = new byte[len];
        in.readFully(respBytes);
        return ClientResponse.parseFrom(respBytes);
    }

    private void listSources() throws IOException {
        ClientResponse r = send("LIST_SOURCES", "");
        if (r.getSuccess()) {
            System.out.println("\n" + r.getData());
        } else {
            System.out.println("\n[ERRO] " + r.getMessage());
        }
    }

    private void sendCommand(Scanner sc) throws IOException {
        String sid = resolveSourceId(sc, "ID da fonte (ex: CARRO-01, SENSOR-AJUSTAVEL-01): ", false);

        System.out.println("Comandos disponíveis:");
        System.out.println("  Genéricos: ENABLE, DISABLE, SET_FREQUENCY, SET_THRESHOLD");
        System.out.println("  Carro:     SET_ENGINE_MODE, CALL_BOX, SET_FUEL_LIMIT");

        String cmd = readNoPipe(sc, "Comando: ", false).toUpperCase(Locale.ROOT);
        String param = readNoPipe(sc, "Parâmetro (ENTER se não houver): ", true);
        String rawValue = readNoPipe(sc, "Valor: ", true);
        String value = rawValue.isEmpty() ? "" : rawValue.toUpperCase(Locale.ROOT);

        ClientResponse r = send("SEND_COMMAND", sid + "|" + cmd + "|" + param + "|" + value);
        System.out.println((r.getSuccess() ? "[OK] " : "[ERRO] ") + r.getMessage()
                + (r.getData().isEmpty() ? "" : " | estado=" + r.getData()));
    }

    private void analytics(Scanner sc) throws IOException {
        System.out.println("1 - Média de uma métrica");
        System.out.println("2 - Desvio padrão de uma métrica");
        System.out.println("3 - Fonte com maior variação em uma métrica");

        String opt = readNonEmpty(sc, "> ");
        String kind;
        switch (opt) {
            case "1": kind = "AVG"; break;
            case "2": kind = "STDDEV"; break;
            case "3": kind = "MAXVAR"; break;
            default:
                System.out.println("[AVISO] Opção inválida. Retornando ao menu.");
                return;
        }

        String sid = resolveSourceId(sc, "ID da fonte (ENTER para todas): ", true);
        String metric = readNoPipe(sc, "Métrica (ex: velocidade, rpm, temperatura_pneu, vibracao): ", false);
        int min = readPositiveInt(sc, "Janela em minutos [padrão 60]: ", 60);

        ClientResponse r = send("ANALYTICS",
                kind + "|" + sid + "|" + metric + "|" + (min * 60_000L));
        if (r.getSuccess()) {
            System.out.println("\n>>> " + r.getData());
        } else {
            System.out.println("\n[ERRO] " + r.getMessage());
        }
    }

    private void history(Scanner sc) throws IOException {
        String metric = readNoPipe(sc, "Métrica: ", false);
        int min = readPositiveInt(sc, "Janela em minutos [padrão 60]: ", 60);
        ClientResponse r = send("HISTORY", metric + "|" + (min * 60_000L));
        if (r.getSuccess()) {
            System.out.println("\n" + r.getData());
        } else {
            System.out.println("\n[ERRO] " + r.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Helpers de prompt (com suporte a /back, rejeição de '|' e validação)
    // ------------------------------------------------------------------

    private boolean isBackCommand(String input) {
        if (input == null) return false;
        String s = input.trim().toLowerCase(Locale.ROOT);
        return s.equals("/back") || s.equals("/cancel") || s.equals("/menu")
            || s.equals("..")    || s.equals("voltar")  || s.equals("cancelar");
    }

    private String resolveSourceId(Scanner sc, String prompt, boolean allowEmpty) throws IOException {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            if (isBackCommand(input)) throw new BackToMenu();

            if (input.isEmpty()) {
                if (allowEmpty) return "";
                System.out.println("[AVISO] Entrada vazia. Digite '?' para listar ou /back para cancelar.");
                continue;
            }
            if (input.equals("?")) {
                listSources();
                continue;
            }
            if (input.contains("|")) {
                System.out.println("[ERRO] O caractere '|' não é permitido no ID.");
                continue;
            }

            List<String> knownIds = extractSourceIds(send("LIST_SOURCES", "").getData());
            for (String id : knownIds) {
                if (id.equalsIgnoreCase(input)) {
                    if (!id.equals(input)) {
                        System.out.println("[INFO] Fonte interpretada como: " + id);
                    }
                    return id;
                }
            }
            System.out.println("[ERRO] Fonte '" + input + "' não encontrada. "
                    + "Digite '?' para listar ou /back para cancelar.");
        }
    }

    private List<String> extractSourceIds(String data) {
        List<String> ids = new ArrayList<>();
        if (data == null || data.isBlank()) return ids;
        for (String line : data.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("(")) continue;
            if (line.contains("domínio= |")) continue; // fantasma GATEWAY-1 (defensivo)
            int idx = line.indexOf(" | ");
            ids.add(idx > 0 ? line.substring(0, idx).trim() : line);
        }
        return ids;
    }

    /** Lê uma string não vazia, com suporte a /back. */
    private String readNonEmpty(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (isBackCommand(input)) throw new BackToMenu();
            if (!input.isEmpty()) return input;
            System.out.println("[AVISO] Entrada vazia. Digite /back para cancelar.");
        }
    }

    /** Lê uma string que não pode conter '|'. Se allowEmpty=false, exige não vazia. */
    private String readNoPipe(Scanner sc, String prompt, boolean allowEmpty) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (isBackCommand(input)) throw new BackToMenu();
            if (input.contains("|")) {
                System.out.println("[ERRO] O caractere '|' não é permitido. Digite novamente.");
                continue;
            }
            if (input.isEmpty() && !allowEmpty) {
                System.out.println("[AVISO] Entrada vazia. Digite /back para cancelar.");
                continue;
            }
            return input;
        }
    }

    /** Lê um inteiro estritamente positivo; ENTER usa o padrão. */
    private int readPositiveInt(Scanner sc, String prompt, int defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (isBackCommand(input)) throw new BackToMenu();
            if (input.isEmpty()) return defaultValue;
            try {
                int v = Integer.parseInt(input);
                if (v <= 0) {
                    System.out.println("[ERRO] O valor deve ser maior que zero. Digite novamente.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("[ERRO] Valor inválido. Digite um inteiro > 0 ou ENTER para "
                        + defaultValue + ".");
            }
        }
    }
}