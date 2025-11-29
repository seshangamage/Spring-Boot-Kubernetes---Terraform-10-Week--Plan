package com.example.resources;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ResourceLimitsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceLimitsApplication.class, args);
    }
}

@RestController
class ResourceController {
    
    private List<byte[]> memoryHog = new ArrayList<>();
    
    @GetMapping("/")
    public String home() {
        return "Resource Limits, QoS & Scheduling Demo!";
    }
    
    @GetMapping("/resources")
    public Map<String, Object> getResources() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        Map<String, Object> resources = new HashMap<>();
        
        // CPU Information
        resources.put("availableProcessors", runtime.availableProcessors());
        
        // Memory Information (in MB)
        Map<String, Long> memory = new HashMap<>();
        memory.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        memory.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        memory.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        memory.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        memory.put("heapUsedMB", heapUsage.getUsed() / (1024 * 1024));
        memory.put("heapMaxMB", heapUsage.getMax() / (1024 * 1024));
        resources.put("memory", memory);
        
        // Container limits (if available)
        Map<String, String> containerInfo = new HashMap<>();
        containerInfo.put("note", "Use kubectl to see actual resource limits");
        resources.put("containerLimits", containerInfo);
        
        return resources;
    }
    
    // Simulate CPU-intensive workload
    @GetMapping("/cpu-load")
    public Map<String, Object> cpuLoad(@RequestParam(defaultValue = "5") int seconds) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (seconds * 1000L);
        
        // CPU-intensive calculation
        double result = 0;
        while (System.currentTimeMillis() < endTime) {
            result += Math.sqrt(Math.random() * 1000);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "CPU load simulation completed");
        response.put("durationMs", duration);
        response.put("requestedSeconds", seconds);
        response.put("result", result);
        
        return response;
    }
    
    // Simulate memory allocation
    @GetMapping("/allocate-memory")
    public Map<String, Object> allocateMemory(@RequestParam(defaultValue = "10") int megabytes) {
        try {
            // Allocate memory in chunks
            for (int i = 0; i < megabytes; i++) {
                byte[] chunk = new byte[1024 * 1024]; // 1 MB
                memoryHog.add(chunk);
            }
            
            Runtime runtime = Runtime.getRuntime();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Memory allocated successfully");
            response.put("allocatedMB", megabytes);
            response.put("totalAllocatedMB", memoryHog.size());
            response.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
            response.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
            
            return response;
        } catch (OutOfMemoryError e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "OutOfMemoryError");
            response.put("message", "Cannot allocate more memory - limit reached");
            response.put("totalAllocatedMB", memoryHog.size());
            
            return response;
        }
    }
    
    // Free allocated memory
    @GetMapping("/free-memory")
    public Map<String, Object> freeMemory() {
        int freedMB = memoryHog.size();
        memoryHog.clear();
        System.gc(); // Suggest garbage collection
        
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Memory freed");
        response.put("freedMB", freedMB);
        response.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        
        return response;
    }
    
    // Simulate memory leak
    @GetMapping("/memory-leak")
    public Map<String, Object> memoryLeak(@RequestParam(defaultValue = "1") int iterations) {
        List<byte[]> leak = new ArrayList<>();
        
        for (int i = 0; i < iterations; i++) {
            leak.add(new byte[1024 * 1024]); // Add 1MB per iteration
        }
        
        // Intentionally keep reference to prevent GC
        memoryHog.addAll(leak);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Memory leak simulated");
        response.put("leakedMB", iterations);
        response.put("totalLeakedMB", memoryHog.size());
        
        return response;
    }
    
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
