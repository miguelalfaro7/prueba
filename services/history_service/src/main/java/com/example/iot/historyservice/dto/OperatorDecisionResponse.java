package com.example.iot.historyservice.dto;

import java.time.Instant;

public record OperatorDecisionResponse(
        Long id,
        String alertId,
        String sensorId,
        String action,
        String routingKey,
        String payload,
        Instant decidedAt) {
}
