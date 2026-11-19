package com.example.iot.alertdetector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SensorStatusEvent(
        @JsonProperty("sensor_id") String sensorId,
        double vibration,
        String status,
        String timestamp) {
}
