package com.example.iot.actiondispatcher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OperatorDecisionRequest(
        @JsonProperty("alert_id") @NotBlank @Size(max = 64) String alertId,
        @JsonProperty("sensor_id") @NotBlank @Size(max = 20) String sensorId,
        @NotNull DecisionAction action) {
}
