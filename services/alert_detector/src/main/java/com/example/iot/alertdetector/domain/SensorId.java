package com.example.iot.alertdetector.domain;

public record SensorId(String value) {
    public SensorId {
        if (value == null || value.isBlank() || value.length() > 20) {
            throw new IllegalArgumentException("sensor_id debe tener entre 1 y 20 caracteres");
        }
    }
}
