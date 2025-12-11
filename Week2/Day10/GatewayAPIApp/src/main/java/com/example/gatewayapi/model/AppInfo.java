package com.example.gatewayapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppInfo {
    private String name;
    private String version;
    private LocalDateTime timestamp;
    private String description;
    private String gatewayType;
}
