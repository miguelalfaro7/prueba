package com.example.iot.sensorproducer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SensorReadingEvent(
        @JsonProperty("sensor_id") String sensorId,
        double vibration,
        String timestamp) {
    public SensorReadingEvent {
        if (sensorId == null || sensorId.isBlank() || sensorId.length() > 20) {
            throw new IllegalArgumentException("sensor_id debe tener entre 1 y 20 caracteres");
        }
        if (!Double.isFinite(vibration) || vibration < 0 || vibration > 100) {
            throw new IllegalArgumentException("La vibración debe estar entre 0 y 100");
        }
        java.time.Instant.parse(timestamp);
    }
}
