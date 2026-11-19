package com.example.iot.historyservice.dto;

import java.time.Instant;

public record SensorReadingResponse(
        Long id,
        String sensorId,
        Double vibration,
        Instant recordedAt,
        Instant storedAt) {
}
