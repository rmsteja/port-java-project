package com.wgu.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class NetworkUtils {
    // Allow only safe argument tokens: letters, digits, underscore, dot, and dash
    private static final Pattern SAFE_TOKEN = Pattern.compile("^[A-Za-z0-9_.-]+$");
    // Whitelist allowed base commands to minimize scope
    private static final List<String> ALLOWED_CMDS = List.of("ping", "traceroute", "nslookup");

    /**
     * Secure wrapper that preserves the original signature but prevents command injection.
     * Instead of invoking a shell, it uses ProcessBuilder with validated tokens and a
     * restricted set of allowed commands.
     */
    public static String runCommand(String command) throws IOException {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Command is empty");
        }
        String base = parts[0];
        if (!ALLOWED_CMDS.contains(base)) {
            throw new IllegalArgumentException("Unsupported command: " + base);
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(base);
        for (int i = 1; i < parts.length; i++) {
            String token = parts[i];
            if (!SAFE_TOKEN.matcher(token).matches()) {
                throw new IllegalArgumentException("Illegal characters in argument: " + token);
            }
            cmd.add(token);
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append(System.lineSeparator());
            }
            return out.toString();
        }
    }

    // Helper to validate hosts for network utilities
    public static boolean isSafeHost(String host) {
        return host != null && !host.isBlank() && SAFE_TOKEN.matcher(host).matches();
    }

    /**
     * Safe ping utility using ProcessBuilder without shell invocation and with input validation.
     */
    public static String pingHost(String host) throws IOException {
        if (!isSafeHost(host)) {
            throw new IllegalArgumentException("Invalid host");
        }
        String os = System.getProperty("os.name").toLowerCase();
        List<String> cmd = new ArrayList<>();
        if (os.contains("win")) {
            cmd.add("ping"); cmd.add("-n"); cmd.add("4"); cmd.add(host);
        } else {
            cmd.add("ping"); cmd.add("-c"); cmd.add("4"); cmd.add(host);
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append(System.lineSeparator());
            }
            return out.toString();
        }
    }
}
