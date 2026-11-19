package com.example.iot.alertdetector.domain;

public record SensorReading(String sensorId, Double vibration) {
    public SensorReading {
        new SensorId(sensorId);
        if (vibration == null || !Double.isFinite(vibration) || vibration < 0 || vibration > 100) {
            throw new IllegalArgumentException("La vibración debe estar entre 0 y 100");
        }
    }
}
