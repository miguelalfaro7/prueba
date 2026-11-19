
package com.example.iot.alertrouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.iot.alertrouter.domain.AlertRoutingPolicy;
import com.example.iot.alertrouter.domain.RoutingDecision;
import com.example.iot.alertrouter.dto.RoutedAlert;
import com.example.iot.alertrouter.dto.SensorAlert;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlertRouter {

    private static final Logger log = LoggerFactory.getLogger(AlertRouter.class);

    private final HumanAlertPublisher publisher;
    private final ObjectMapper objectMapper;
    private final AlertRoutingPolicy routingPolicy = new AlertRoutingPolicy();

    public AlertRouter(HumanAlertPublisher publisher, ObjectMapper objectMapper) {
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {"alerts_critical", "alerts_warning"},
            groupId = "alert-router-group"
    )
    public void routeAlert(String message) {

        try {
            SensorAlert alert = objectMapper.readValue(message, SensorAlert.class);
            RoutingDecision decision = routingPolicy.decide(alert.type());

            RoutedAlert routedAlert = new RoutedAlert(
                    alert.alertId(),
                    alert.sensorId(),
                    decision.severity().name(),
                    alert.vibration(),
                    alert.timestamp(),
                    decision.actions().stream()
                            .map(action -> action.externalName())
                            .toList()
            );

            publisher.publish(routedAlert);

            log.info("[RABBITMQ] Alerta {} enviada: {}", decision.severity(), alert.sensorId());

        } catch (JsonProcessingException e) {
            log.warn("Error procesando JSON de alerta: {}", e.getOriginalMessage());
        } catch (IllegalArgumentException e) {
            log.warn("{}", e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado procesando o publicando alerta", e);
        }
    }
}
