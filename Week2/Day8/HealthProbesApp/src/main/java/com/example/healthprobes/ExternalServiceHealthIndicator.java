package com.example.healthprobes;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Simulate external service health check (e.g., payment gateway, email service)
        boolean serviceAvailable = checkExternalService();
        
        if (serviceAvailable) {
            return Health.up()
                    .withDetail("externalService", "Available")
                    .withDetail("responseTime", "120ms")
                    .build();
        } else {
            return Health.down()
                    .withDetail("externalService", "Unavailable")
                    .withDetail("error", "Timeout after 5 seconds")
                    .build();
        }
    }
    
    private boolean checkExternalService() {
        // Simulate external service check
        // In real scenario, this would make HTTP call to external API
        return true; // Always available for demo
    }
}
