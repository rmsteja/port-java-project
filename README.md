# Enterprise Application

A Java application providing various enterprise services including user authentication, data processing, caching, network utilities, and file management.

## Project Structure

### 1. User Authentication Service
**Location:** `UserService.java`

Provides user authentication functionality using XML-based user storage. Authenticates users by matching username and password against stored credentials.

**Usage:**
```java
UserService userService = new UserService();
boolean authenticated = userService.authenticate("username", "password");
```

### 2. Data Processing Utility
**Location:** `DataProcessor.java`

Utility class for processing binary data. Provides methods for copying data to buffers and managing byte arrays.

**Usage:**
```java
DataProcessor processor = new DataProcessor();
processor.processData(byteArray);
```

### 3. Cache Management Service
**Location:** `CacheManager.java`

Manages object caching using Java serialization. Provides functionality to save and load cached objects from files or byte arrays.

**Usage:**
```java
CacheManager cache = new CacheManager();
cache.saveToCache(object, "cache.ser");
Object loaded = cache.loadFromCache("cache.ser");
```

### 4. Network Utilities
**Location:** `NetworkUtils.java`

Utility class for network operations including host connectivity checks and file reading operations.

**Usage:**
```java
NetworkUtils network = new NetworkUtils();
String result = network.pingHost("example.com");
```

### 5. File Management Service
**Location:** `FileManager.java`

Manages file operations, URL fetching, database connections, and network socket operations.

**Usage:**
```java
FileManager fileManager = new FileManager();
String content = fileManager.readFile("data.txt");
String urlContent = fileManager.fetchUrl("http://example.com");
```

## Building and Running

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Build the Project
```bash
mvn clean compile
```

### Run the Application
```bash
mvn exec:java -Dexec.mainClass="com.wgu.app.Application"
```

Or compile and run manually:
```bash
javac -d target/classes src/main/java/com/wgu/app/*.java
java -cp target/classes com.wgu.app.Application
```

## Project Structure
```
port-java-project/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── wgu/
                    └── app/
                        ├── Application.java
                        ├── UserService.java
                        ├── DataProcessor.java
                        ├── CacheManager.java
                        ├── NetworkUtils.java
                        └── FileManager.java
```

## Features

- User authentication service
- Binary data processing
- Object caching with serialization
- Network connectivity utilities
- File and resource management

