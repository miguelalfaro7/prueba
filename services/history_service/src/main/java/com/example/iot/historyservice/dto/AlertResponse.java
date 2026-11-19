package com.example.iot.historyservice.dto;

import java.time.Instant;

public record AlertResponse(
        Long id,
        String alertId,
        String sensorId,
        String severity,
        Double vibrationValue,
        Instant occurredAt,
        String payload,
        Instant createdAt) {
}
