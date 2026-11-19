package com.example.iot.alertrouter.domain;

public enum AlertAction {
    SHUT_DOWN_IMMEDIATELY("APAGADO_INMEDIATO"),
    IGNORE_FOR_10_MINUTES("IGNORAR_10_MINUTOS"),
    SCHEDULE_MAINTENANCE_NOW("PROGRAMAR_MANTENIMIENTO_AHORA"),
    ACKNOWLEDGE_AND_WAIT_24_HOURS("RECONOCER_Y_ESPERAR_24H");

    private final String externalName;

    AlertAction(String externalName) {
        this.externalName = externalName;
    }

    public String externalName() {
        return externalName;
    }
}
