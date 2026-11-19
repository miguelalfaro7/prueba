package com.example.iot.historyservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SensorReadingEvent(
        @JsonProperty("sensor_id") @JsonAlias({"sensorId", "sensor"}) String sensorId,
        @JsonProperty("vibration") @JsonAlias({"vibration_value", "vibrationValue", "value"}) Double vibration,
        String timestamp) {
}
