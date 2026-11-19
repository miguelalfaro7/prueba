package com.example.iot.alertdetector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SensorAlert(
        @JsonProperty("alert_id") String alertId,
        @JsonProperty("sensor_id") String sensorId,
        String type,
        double vibration,
        String timestamp) {
}
