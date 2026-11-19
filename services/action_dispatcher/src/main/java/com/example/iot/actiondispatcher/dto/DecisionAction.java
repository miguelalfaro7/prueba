package com.example.iot.actiondispatcher.dto;

public enum DecisionAction {
    APAGADO_INMEDIATO("actuator"),
    IGNORAR_10_MINUTOS("actuator"),
    PROGRAMAR_MANTENIMIENTO_AHORA("maintenance"),
    RECONOCER_Y_ESPERAR_24H("maintenance");

    private final String routingKey;

    DecisionAction(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }
}
