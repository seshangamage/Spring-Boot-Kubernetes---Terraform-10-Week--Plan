package com.example.healthprobes;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final Random random = new Random();
    
    @Override
    public Health health() {
        // Simulate database health check
        boolean dbAvailable = checkDatabaseConnection();
        
        if (dbAvailable) {
            return Health.up()
                    .withDetail("database", "Connected")
                    .withDetail("connection_pool", "Available")
                    .build();
        } else {
            return Health.down()
                    .withDetail("database", "Connection failed")
                    .withDetail("error", "Unable to reach database")
                    .build();
        }
    }
    
    private boolean checkDatabaseConnection() {
        // Simulate database connectivity check
        // In real scenario, this would ping the database
        return random.nextDouble() > 0.1; // 90% success rate
    }
}
