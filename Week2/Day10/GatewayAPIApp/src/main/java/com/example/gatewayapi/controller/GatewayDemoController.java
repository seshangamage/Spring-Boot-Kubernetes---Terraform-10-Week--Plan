package com.example.gatewayapi.controller;

import com.example.gatewayapi.model.AppInfo;
import com.example.gatewayapi.model.HealthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class GatewayDemoController {

    @Value("${app.name:Gateway API Demo}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/info")
    public ResponseEntity<AppInfo> getInfo(HttpServletRequest request) {
        log.info("Received request for /api/info from {}", request.getRemoteAddr());
        
        AppInfo info = AppInfo.builder()
                .name(appName)
                .version(appVersion)
                .timestamp(LocalDateTime.now())
                .description("Spring Boot application exposed via Kubernetes Gateway API")
                .gatewayType("Traefik")
                .build();
        
        return ResponseEntity.ok(info);
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        log.info("Health check requested");
        
        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .message("Application is healthy and accessible via Gateway API")
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/headers")
    public ResponseEntity<Map<String, String>> getHeaders(HttpServletRequest request) {
        log.info("Headers endpoint called");
        
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> 
            headers.put(headerName, request.getHeader(headerName))
        );
        
        return ResponseEntity.ok(headers);
    }

    @GetMapping("/path/{value}")
    public ResponseEntity<Map<String, String>> pathVariable(@PathVariable String value) {
        log.info("Path variable endpoint called with value: {}", value);
        
        Map<String, String> response = new HashMap<>();
        response.put("path", value);
        response.put("message", "Path routing via HTTPRoute works!");
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> postData(@RequestBody Map<String, Object> data) {
        log.info("POST request received with data: {}", data);
        
        Map<String, Object> response = new HashMap<>();
        response.put("received", data);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "processed");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/gateway-info")
    public ResponseEntity<Map<String, String>> getGatewayInfo(HttpServletRequest request) {
        log.info("Gateway info endpoint called");
        
        Map<String, String> gatewayInfo = new HashMap<>();
        gatewayInfo.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        gatewayInfo.put("X-Forwarded-Proto", request.getHeader("X-Forwarded-Proto"));
        gatewayInfo.put("X-Forwarded-Host", request.getHeader("X-Forwarded-Host"));
        gatewayInfo.put("X-Real-IP", request.getHeader("X-Real-IP"));
        gatewayInfo.put("Host", request.getHeader("Host"));
        gatewayInfo.put("Protocol", request.getProtocol());
        gatewayInfo.put("Scheme", request.getScheme());
        gatewayInfo.put("ServerName", request.getServerName());
        
        return ResponseEntity.ok(gatewayInfo);
    }
}
