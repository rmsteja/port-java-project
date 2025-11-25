package com.wgu.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Utility class for network operations.
 */
public class NetworkUtils {
    
    /**
     * Pings a host to check connectivity.
     */
    public String pingHost(String host) {
        try {
            String command = "ping -c 1 " + host;
            
            Process process = Runtime.getRuntime().exec(command);
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
    
    /**
     * Reads a file using system command.
     */
    public String readFile(String filename) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cat", filename);
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
}

