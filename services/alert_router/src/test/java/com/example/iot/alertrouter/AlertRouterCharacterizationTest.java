package com.example.iot.alertrouter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class AlertRouterCharacterizationTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AlertRouter alertRouter;

    @BeforeEach
    void setUp() {
        alertRouter = new AlertRouter(new RabbitHumanAlertPublisher(rabbitTemplate, objectMapper), objectMapper);
    }

    @Test
    void routesCriticalAlertWithOrderedActionsAndExistingJsonContract() throws Exception {
        alertRouter.routeAlert(alertJson("CRITICAL"));

        JsonNode routedAlert = capturePublishedJson();
        assertEquals("alert-1", routedAlert.get("alert_id").asText());
        assertEquals("sensor_A", routedAlert.get("sensor_id").asText());
        assertEquals("CRITICAL", routedAlert.get("type").asText());
        assertEquals(91.25, routedAlert.get("vibration").asDouble());
        assertEquals("2026-11-18T12:00:00Z", routedAlert.get("timestamp").asText());
        assertEquals("APAGADO_INMEDIATO", routedAlert.get("actions").get(0).asText());
        assertEquals("IGNORAR_10_MINUTOS", routedAlert.get("actions").get(1).asText());
        assertEquals(6, routedAlert.size());
    }

    @Test
    void routesWarningWithItsExistingActionOrder() throws Exception {
        alertRouter.routeAlert(alertJson("WARNING"));

        JsonNode routedAlert = capturePublishedJson();
        assertEquals("PROGRAMAR_MANTENIMIENTO_AHORA", routedAlert.get("actions").get(0).asText());
        assertEquals("RECONOCER_Y_ESPERAR_24H", routedAlert.get("actions").get(1).asText());
    }

    @Test
    void ignoresUnknownSeverity() {
        alertRouter.routeAlert(alertJson("UNKNOWN"));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
    }

    @Test
    void preservesCurrentHandlingOfMissingFieldsForKnownSeverity() throws Exception {
        alertRouter.routeAlert("""
                {"type":"CRITICAL"}
                """);

        JsonNode routedAlert = capturePublishedJson();
        assertTrue(routedAlert.get("alert_id").isNull());
        assertTrue(routedAlert.get("sensor_id").isNull());
        assertEquals(0.0, routedAlert.get("vibration").asDouble());
        assertTrue(routedAlert.get("timestamp").isNull());
    }

    @Test
    void malformedJsonAndRabbitFailureAreSwallowedAsBefore() {
        assertDoesNotThrow(() -> alertRouter.routeAlert("not-json"));

        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(
                        eq(RabbitMQConfig.EXCHANGE_NAME),
                        (String) eq(""),
                        (Object) any()
                );
        assertDoesNotThrow(() -> alertRouter.routeAlert(alertJson("CRITICAL")));
    }

    private JsonNode capturePublishedJson() throws Exception {
        ArgumentCaptor<String> exchange = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(exchange.capture(), routingKey.capture(), payload.capture());
        assertEquals("human_alerts", exchange.getValue());
        assertEquals("", routingKey.getValue());
        return objectMapper.readTree(payload.getValue());
    }

    private String alertJson(String type) {
        return """
                {"alert_id":"alert-1","sensor_id":"sensor_A","type":"%s","vibration":91.25,"timestamp":"2026-11-18T12:00:00Z"}
                """.formatted(type);
    }
}
