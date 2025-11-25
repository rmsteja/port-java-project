package com.wgu.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Set;

/**
 * Secure FileManager with validation to prevent resource injection.
 * - Enforces a base directory and normalizes user-supplied paths to block path traversal.
 * - Provides optional, restricted URL access to mitigate SSRF via host allowlisting and private IP blocking.
 */
public class FileManager {
    private final Path baseDir;
    private final Set<String> allowedHosts; // Optional allowlist for network resources

    /**
     * Construct a FileManager rooted at a base directory. All file operations are confined within this directory.
     * @param baseDir The base directory root for file operations.
     * @param allowedHosts Allowed hostnames for network access. Provide an empty set to disable URL access.
     */
    public FileManager(Path baseDir, Set<String> allowedHosts) {
        this.baseDir = Objects.requireNonNull(baseDir, "baseDir").toAbsolutePath().normalize();
        this.allowedHosts = Objects.requireNonNull(allowedHosts, "allowedHosts");
    }

    /**
     * Resolve a user-supplied path safely within baseDir, preventing path traversal.
     */
    private Path resolveSafe(String userInputPath) throws IOException {
        if (userInputPath == null || userInputPath.isBlank()) {
            throw new IllegalArgumentException("Invalid path input");
        }
        Path candidate = baseDir.resolve(userInputPath).normalize();
        if (!candidate.startsWith(baseDir)) {
            throw new SecurityException("Path traversal detected; access outside base directory is prohibited");
        }
        return candidate;
    }

    /**
     * Read a file within the base directory using a user-supplied path, with traversal protection.
     */
    public String readFile(String userPath) throws IOException {
        Path path = resolveSafe(userPath);
        return Files.readString(path);
    }

    /**
     * Write content to a file within the base directory using a user-supplied path, with traversal protection.
     */
    public void writeFile(String userPath, String content) throws IOException {
        Path path = resolveSafe(userPath);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content == null ? "" : content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Optional: Open a URL stream with SSRF mitigations. If allowedHosts is empty, URL access is disabled.
     */
    public InputStream openUrl(URL url) throws IOException {
        if (allowedHosts.isEmpty()) {
            throw new SecurityException("URL access is disabled");
        }
        if (url == null) {
            throw new IllegalArgumentException("URL must not be null");
        }
        String host = url.getHost();
        if (host == null || host.isBlank() || !allowedHosts.contains(host)) {
            throw new SecurityException("Host not allowed");
        }
        // Block access to internal or private addresses to mitigate SSRF
        InetAddress addr = InetAddress.getByName(host);
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
            throw new SecurityException("Internal/private addresses are blocked");
        }
        return url.openStream();
    }
}

