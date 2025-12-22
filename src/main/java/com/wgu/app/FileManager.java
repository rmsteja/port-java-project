package com.wgu.app;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * Secure FileManager with validation to prevent resource injection:
 * - Validates user-supplied paths to prevent path traversal and arbitrary file access
 * - Validates URLs and blocks SSRF to localhost, private and link-local networks
 */
public class FileManager {
    // Restrict file operations to a safe base directory (can be customized)
    private static final Path BASE_DIR = Paths.get(System.getProperty("user.home"), "appdata");

    static {
        try {
            Files.createDirectories(BASE_DIR);
        } catch (IOException ignored) {
            // If directory can't be created, operations will still validate against intended base
        }
    }

    // ===================== Public API (kept common/expected names) =====================

    /**
     * Returns an InputStream for a user-supplied file path, with safe validation.
     */
    public static InputStream getFileInputStream(String userPath) throws IOException {
        Path safe = sanitizePath(userPath);
        return new BufferedInputStream(Files.newInputStream(safe, StandardOpenOption.READ));
    }

    /**
     * Reads a text file from a user-supplied path, with safe validation.
     */
    public static String readFile(String userPath) throws IOException {
        Path safe = sanitizePath(userPath);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(safe, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Writes bytes to a file at a user-supplied path, with safe validation.
     */
    public static void writeFile(String userPath, byte[] data) throws IOException {
        Path safe = sanitizePath(userPath);
        Files.createDirectories(safe.getParent());
        Files.write(safe, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * Downloads bytes from a URL provided by the user using secure validation to prevent SSRF.
     */
    public static byte[] downloadFromUrl(String userUrl) throws IOException {
        URL url = validateUrl(userUrl);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false); // prevent redirect-based SSRF
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        // Block redirects to disallowed hosts
        int status = conn.getResponseCode();
        if (isRedirect(status)) {
            String location = conn.getHeaderField("Location");
            if (location == null) {
                throw new IOException("Redirect without Location header");
            }
            URL redirected = validateUrl(location);
            conn.disconnect();
            conn = (HttpURLConnection) redirected.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
        }

        try (InputStream is = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            return baos.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    // ===================== Validation helpers =====================

    /**
     * Validates and normalizes a user-supplied path, ensuring it stays within BASE_DIR and isn't absolute,
     * doesn't contain null bytes, and prevents path traversal.
     */
    private static Path sanitizePath(String userPath) throws IOException {
        if (userPath == null) throw new IOException("Path cannot be null");
        if (userPath.length() > 4096) throw new IOException("Path too long");
        if (userPath.indexOf('\0') >= 0) throw new IOException("Invalid path");

        // Disallow Windows drive prefixes or absolute paths
        Path input = Paths.get(userPath);
        if (input.isAbsolute()) throw new IOException("Absolute paths are not allowed");

        // Resolve against base and normalize to prevent traversal
        Path resolved = BASE_DIR.resolve(input).normalize();
        if (!resolved.startsWith(BASE_DIR)) {
            throw new IOException("Path traversal detected");
        }
        return resolved;
    }

    /**
     * Validates the user-supplied URL to prevent SSRF: only http/https, no localhost, loopback, private or link-local.
     */
    private static URL validateUrl(String userUrl) throws IOException {
        if (userUrl == null) throw new IOException("URL cannot be null");
        if (userUrl.length() > 4096) throw new IOException("URL too long");

        URI uri;
        try {
            uri = URI.create(userUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL", e);
        }

        String scheme = uri.getScheme();
        if (scheme == null) throw new IOException("URL must include scheme");
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IOException("Only http/https are allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isEmpty()) throw new IOException("URL must include host");

        // Normalize international domain names
        host = IDN.toASCII(host);
        InetAddress addr = InetAddress.getByName(host);
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            throw new IOException("Local addresses are not allowed");
        }
        if (addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
            throw new IOException("Private or link-local addresses are not allowed");
        }

        // Block common private ranges explicitly (IPv4)
        byte[] ip = addr.getAddress();
        if (ip.length == 4) {
            int b0 = Byte.toUnsignedInt(ip[0]);
            int b1 = Byte.toUnsignedInt(ip[1]);
            if (b0 == 10 || (b0 == 172 && (b1 >= 16 && b1 <= 31)) || (b0 == 192 && b1 == 168)) {
                throw new IOException("Private network addresses are not allowed");
            }
        }

        try {
            return uri.toURL();
        } catch (Exception e) {
            throw new IOException("Invalid URL conversion", e);
        }
    }

    private static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM ||
               status == HttpURLConnection.HTTP_MOVED_TEMP ||
               status == HttpURLConnection.HTTP_SEE_OTHER ||
               status == 307 || status == 308;
    }
}

