package com.example.iot.maintenanceworker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OperatorDecision(
        @JsonProperty("alert_id") String alertId,
        @JsonProperty("sensor_id") String sensorId,
        DecisionAction action) {

    public boolean hasIdentity() {
        return alertId != null && !alertId.isBlank()
                && sensorId != null && !sensorId.isBlank();
    }
}
