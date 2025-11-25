package com.wgu.app;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages file operations and resource access.
 */
public class FileManager {
    
    /**
     * Reads content from a file.
     */
    public String readFile(String filename) {
        try {
            Path filePath = Paths.get(filename);
            return new String(Files.readAllBytes(filePath));
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Fetches content from a URL.
     */
    public String fetchUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            
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
    
    /**
     * Establishes a database connection.
     */
    public Connection connectDatabase(String connectionString) {
        try {
            return DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Connects to a network socket.
     */
    public void connectSocket(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            System.out.println("Connected to " + host + ":" + port);
            socket.close();
        } catch (Exception e) {
            System.err.println("Socket connection error: " + e.getMessage());
        }
    }
}

