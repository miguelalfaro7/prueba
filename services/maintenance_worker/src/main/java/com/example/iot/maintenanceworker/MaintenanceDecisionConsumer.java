package com.example.iot.maintenanceworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.iot.maintenanceworker.dto.DecisionAction;
import com.example.iot.maintenanceworker.dto.OperatorDecision;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceDecisionConsumer {

    private final ObjectMapper objectMapper;

    public MaintenanceDecisionConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queuesToDeclare = @Queue(value = "maintenance_queue", durable = "true"))
    public void receiveDecision(String message) throws Exception {

        OperatorDecision decision = objectMapper.readValue(
                message,
                OperatorDecision.class
        );

        if (!decision.hasIdentity() || decision.action() == null) {
            throw new IllegalArgumentException(
                "La decision no contiene alerta, sensor o accion validos"
            );
        }

        System.out.println("[RABBITMQ] Decision recibida: " + message);

        switch (decision.action()) {

            case PROGRAMAR_MANTENIMIENTO_AHORA:
                System.out.println(
                    "[MANTENIMIENTO] Solicitud de mantenimiento inmediato para: "
                    + decision.sensorId()
                );
                break;

            case RECONOCER_Y_ESPERAR_24H:
                System.out.println(
                    "[MANTENIMIENTO] Ejecutando mantenimiento diferido para: "
                    + decision.sensorId()
                );
                break;

            default:
                throw new IllegalArgumentException("Accion no aplicable a mantenimiento: " + decision.action());
        }

        System.out.println(
            "[MANTENIMIENTO] Decision procesada para alerta: " + decision.alertId()
        );
    }
}
