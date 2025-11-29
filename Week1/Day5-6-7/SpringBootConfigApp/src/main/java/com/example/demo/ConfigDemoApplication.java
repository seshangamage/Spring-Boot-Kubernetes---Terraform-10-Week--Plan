package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@SpringBootApplication
public class ConfigDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigDemoApplication.class, args);
    }
}

@RestController
class ConfigController {
    
    // Injecting values from ConfigMap
    @Value("${APP_NAME:DefaultApp}")
    private String appName;
    
    @Value("${LOG_LEVEL:INFO}")
    private String logLevel;
    
    // Injecting values from Secret
    @Value("${DB_USER:defaultuser}")
    private String dbUser;
    
    @Value("${DB_PASS:defaultpass}")
    private String dbPassword;
    
    @GetMapping("/")
    public String home() {
        return "Spring Boot ConfigMap, Secrets & Volumes Demo!";
    }
    
    @GetMapping("/config")
    public String getConfig() {
        return String.format(
            "Application Name: %s\nLog Level: %s\nDB User: %s\nDB Password: %s",
            appName, logLevel, dbUser, "***" + dbPassword.substring(Math.max(0, dbPassword.length() - 3))
        );
    }
    
    @GetMapping("/health")
    public String health() {
        return "Service is running!";
    }
    
    @GetMapping("/write-log")
    public String writeLog() {
        String logDir = "/app/logs";
        String logFile = logDir + "/app.log";
        
        try {
            // Create directory if it doesn't exist
            File directory = new File(logDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Write log entry
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(LocalDateTime.now() + " - Log entry from " + appName + "\n");
            writer.close();
            
            return "Log written successfully to " + logFile;
        } catch (IOException e) {
            return "Error writing log: " + e.getMessage();
        }
    }
    
    @GetMapping("/read-logs")
    public String readLogs() {
        String logFile = "/app/logs/app.log";
        
        try {
            if (Files.exists(Paths.get(logFile))) {
                String content = new String(Files.readAllBytes(Paths.get(logFile)));
                return "=== Log File Contents ===\n" + content;
            } else {
                return "Log file not found. Use /write-log to create entries.";
            }
        } catch (IOException e) {
            return "Error reading log: " + e.getMessage();
        }
    }
}
