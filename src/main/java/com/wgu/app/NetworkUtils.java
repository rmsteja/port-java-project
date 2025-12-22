package com.wgu.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Secure network utility helpers.
 *
 * Fix: prevent command injection by never passing concatenated strings to a shell.
 * Use ProcessBuilder with a fixed command + validated arguments, and strict whitelisting
 * for host input. Also capture output safely and limit process execution.
 */
public final class NetworkUtils {
    private static final Pattern HOST_PATTERN = Pattern.compile(
            "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|" + // domain name
            "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))$"); // IPv4

    private NetworkUtils() {}

    /** Validate host to allow only domain names or IPv4 addresses. */
    private static String validateHost(String host) {
        if (host == null) {
            throw new IllegalArgumentException("host must not be null");
        }
        String trimmed = host.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("host must not be empty");
        }
        // Basic length limit to avoid resource abuse
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("host too long");
        }
        if (!HOST_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("invalid host format");
        }
        return trimmed;
    }

    /**
     * Ping a host safely using the OS ping utility.
     * This method avoids command injection by using a fixed argv form.
     *
     * @param host domain or IPv4
     * @param count number of echo requests to send (1-5)
     * @return stdout of the ping command
     */
    public static String ping(String host, int count) throws IOException, InterruptedException {
        String safeHost = validateHost(host);
        int c = Math.max(1, Math.min(5, count));

        List<String> cmd = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cmd.add("ping");
            cmd.add("-n");
            cmd.add(String.valueOf(c));
            cmd.add(safeHost);
        } else {
            cmd.add("ping");
            cmd.add("-c");
            cmd.add(String.valueOf(c));
            cmd.add(safeHost);
        }
        return runProcess(cmd, 15_000);
    }

    /**
     * Run traceroute safely.
     * On Windows uses "tracert"; on Unix-like uses "traceroute".
     */
    public static String traceroute(String host) throws IOException, InterruptedException {
        String safeHost = validateHost(host);
        List<String> cmd = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cmd.add("tracert");
            cmd.add(safeHost);
        } else {
            cmd.add("traceroute");
            cmd.add(safeHost);
        }
        return runProcess(cmd, 30_000);
    }

    /**
     * Execute a process with the provided argv without invoking a shell.
     * Captures stdout and stderr, and enforces a timeout.
     */
    private static String runProcess(List<String> argv, long timeoutMillis) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(argv);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append(System.lineSeparator());
            }
        }

        boolean finished = p.waitFor(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("process timeout");
        }
        int exit = p.exitValue();
        if (exit != 0 && out.length() == 0) {
            throw new IOException("command failed with exit code " + exit);
        }
        return out.toString();
    }
}

