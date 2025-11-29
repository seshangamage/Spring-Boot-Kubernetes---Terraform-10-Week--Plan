package com.example.healthprobes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class HealthProbesApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthProbesApplication.class, args);
    }
}

@RestController
class HealthController {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    private boolean isReady = true;
    private boolean isLive = true;
    
    @GetMapping("/")
    public String home() {
        return "Health Probes Demo - Liveness & Readiness Checks!";
    }
    
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Health Probes Demo");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("ready", isReady);
        response.put("live", isLive);
        return response;
    }
    
    // Endpoint to simulate application NOT ready (e.g., dependencies not available)
    @GetMapping("/simulate/notready")
    public String simulateNotReady() {
        isReady = false;
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
        return "Application marked as NOT READY. Readiness probe will fail.";
    }
    
    // Endpoint to restore readiness
    @GetMapping("/simulate/ready")
    public String simulateReady() {
        isReady = true;
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
        return "Application marked as READY. Readiness probe will pass.";
    }
    
    // Endpoint to simulate application crash (liveness failure)
    @GetMapping("/simulate/broken")
    public String simulateBroken() {
        isLive = false;
        AvailabilityChangeEvent.publish(eventPublisher, this, LivenessState.BROKEN);
        return "Application marked as BROKEN. Liveness probe will fail. Pod will be restarted.";
    }
    
    // Endpoint to simulate slow startup
    @GetMapping("/simulate/slowstart")
    public String simulateSlowStart() throws InterruptedException {
        Thread.sleep(45000); // Sleep for 45 seconds
        return "Slow startup completed!";
    }
    
    // Endpoint to check current health status
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("readiness", isReady ? "READY" : "NOT_READY");
        response.put("liveness", isLive ? "ALIVE" : "BROKEN");
        response.put("message", "Use /actuator/health/liveness and /actuator/health/readiness for K8s probes");
        return response;
    }
}
