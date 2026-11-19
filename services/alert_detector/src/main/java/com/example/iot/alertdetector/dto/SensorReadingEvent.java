package com.example.iot.alertdetector.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SensorReadingEvent(
        @JsonProperty("sensor_id") @JsonAlias({"sensorId", "sensor"}) String sensorId,
        Double vibration,
        String timestamp) {
}
