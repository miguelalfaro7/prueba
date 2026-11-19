package com.example.iot.alertrouter.domain;

public enum AlertSeverity {
    CRITICAL,
    WARNING;

    public static AlertSeverity from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tipo de alerta desconocido: null");
        }

        return switch (value) {
            case "CRITICAL" -> CRITICAL;
            case "WARNING" -> WARNING;
            default -> throw new IllegalArgumentException(
                    "Tipo de alerta desconocido: " + value
            );
        };
    }
}
