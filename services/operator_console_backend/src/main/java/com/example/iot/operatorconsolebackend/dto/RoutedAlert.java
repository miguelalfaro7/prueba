package com.example.iot.operatorconsolebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RoutedAlert(
        @JsonProperty("alert_id") String alertId,
        @JsonProperty("sensor_id") String sensorId,
        String type,
        double vibration,
        String timestamp,
        List<String> actions) {
}
