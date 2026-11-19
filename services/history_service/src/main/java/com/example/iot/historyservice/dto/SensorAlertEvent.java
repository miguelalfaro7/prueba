package com.example.iot.historyservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SensorAlertEvent(
        @JsonProperty("alert_id") @JsonAlias("alertId") String alertId,
        @JsonProperty("sensor_id") @JsonAlias({"sensorId", "sensor"}) String sensorId,
        String type,
        @JsonProperty("vibration") @JsonAlias({"vibration_value", "vibrationValue", "value"}) Double vibration,
        String timestamp) {
}
