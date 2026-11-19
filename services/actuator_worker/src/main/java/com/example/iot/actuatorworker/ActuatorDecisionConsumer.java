package com.example.iot.actuatorworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.iot.actuatorworker.dto.DecisionAction;
import com.example.iot.actuatorworker.dto.OperatorDecision;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ActuatorDecisionConsumer {

    private final ObjectMapper objectMapper;

    public ActuatorDecisionConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queuesToDeclare = @Queue(value = "critical_actions_queue", durable = "true"))
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

        System.out.println(
                "[RABBITMQ] Decision recibida: " + message
        );

        switch (decision.action()) {

            case APAGADO_INMEDIATO:

                System.out.println(
                        "[ACTUADOR] Apagado inmediato simulado del equipo: "
                                + decision.sensorId()
                );

                break;

            case IGNORAR_10_MINUTOS:

                System.out.println(
                        "[ACTUADOR] Se registra una pausa de 10 minutos para: "
                                + decision.sensorId()
                );

                break;

            default:

                throw new IllegalArgumentException("Accion no aplicable al actuador: " + decision.action());
        }

        System.out.println(
                "[ACTUADOR] Decision procesada para alerta: " + decision.alertId()
        );
    }
}
