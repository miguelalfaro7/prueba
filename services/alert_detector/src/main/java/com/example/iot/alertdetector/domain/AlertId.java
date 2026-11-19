package com.example.iot.alertdetector.domain;

import java.util.UUID;

public record AlertId(UUID value) {
    public AlertId {
        if (value == null) {
            throw new IllegalArgumentException("alert_id es obligatorio");
        }
    }

    public static AlertId create() {
        return new AlertId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
