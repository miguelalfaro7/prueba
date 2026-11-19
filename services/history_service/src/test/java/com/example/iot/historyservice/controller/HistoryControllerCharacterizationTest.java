package com.example.iot.historyservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.iot.historyservice.dto.AlertResponse;
import com.example.iot.historyservice.dto.OperatorDecisionResponse;
import com.example.iot.historyservice.dto.SensorReadingResponse;
import com.example.iot.historyservice.entity.Alert;
import com.example.iot.historyservice.entity.OperatorDecision;
import com.example.iot.historyservice.entity.SensorReading;
import com.example.iot.historyservice.repository.AlertRepository;
import com.example.iot.historyservice.repository.OperatorDecisionRepository;
import com.example.iot.historyservice.repository.SensorReadingRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class HistoryControllerCharacterizationTest {

    @Mock
    private AlertRepository alerts;
    @Mock
    private OperatorDecisionRepository decisions;
    @Mock
    private SensorReadingRepository readings;

    private HistoryController controller;

    @BeforeEach
    void setUp() {
        controller = new HistoryController(alerts, decisions, readings);
    }

    @Test
    void returnsExistingResponseShapesAndClampsLimits() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        Alert alert = new Alert("sensor_A", "CRITICAL", 92.0, "{}", "alert-1", timestamp);
        OperatorDecision decision = new OperatorDecision(
                "sensor_A", "APAGADO_INMEDIATO", "actuator", "{}", "alert-1");
        SensorReading reading = new SensorReading("sensor_A", 92.0, timestamp);
        when(alerts.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of(alert));
        when(decisions.findBySensorIdOrderByDecidedAtDesc("sensor_A", PageRequest.of(0, 500)))
                .thenReturn(List.of(decision));
        when(readings.findAllByOrderByRecordedAtDesc(PageRequest.of(0, 1)))
                .thenReturn(List.of(reading));

        List<AlertResponse> alertResponses = controller.alerts(null, 501);
        List<OperatorDecisionResponse> decisionResponses = controller.decisions("sensor_A", 900);
        List<SensorReadingResponse> readingResponses = controller.readings(null, 0);

        assertEquals("alert-1", alertResponses.get(0).alertId());
        assertEquals("sensor_A", decisionResponses.get(0).sensorId());
        assertEquals("APAGADO_INMEDIATO", decisionResponses.get(0).action());
        assertEquals(92.0, readingResponses.get(0).vibration());
        verify(alerts).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 500));
        verify(decisions).findBySensorIdOrderByDecidedAtDesc("sensor_A", PageRequest.of(0, 500));
        verify(readings).findAllByOrderByRecordedAtDesc(PageRequest.of(0, 1));
    }

    @Test
    void responseDtosKeepTheExistingJsonPropertyNames() throws Exception {
        Alert alert = new Alert("sensor_A", "CRITICAL", 92.0, "{}", "alert-1",
                Instant.parse("2026-01-01T00:00:00Z"));
        OperatorDecision decision = new OperatorDecision(
                "sensor_A", "APAGADO_INMEDIATO", "actuator", "{}", "alert-1");
        SensorReading reading = new SensorReading(
                "sensor_A", 92.0, Instant.parse("2026-01-01T00:00:00Z"));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        JsonNode alertJson = mapper.valueToTree(new AlertResponse(
                alert.getId(), alert.getAlertId(), alert.getSensorId(), alert.getSeverity(),
                alert.getVibrationValue(), alert.getOccurredAt(), alert.getPayload(), alert.getCreatedAt()));
        JsonNode decisionJson = mapper.valueToTree(new OperatorDecisionResponse(
                decision.getId(), decision.getAlertId(), decision.getSensorId(), decision.getAction(),
                decision.getRoutingKey(), decision.getPayload(), decision.getDecidedAt()));
        JsonNode readingJson = mapper.valueToTree(new SensorReadingResponse(
                reading.getId(), reading.getSensorId(), reading.getVibration(),
                reading.getRecordedAt(), reading.getStoredAt()));

        assertEquals(8, alertJson.size());
        assertEquals(7, decisionJson.size());
        assertEquals(5, readingJson.size());
        assertEquals("alert-1", alertJson.get("alertId").asText());
        assertEquals("actuator", decisionJson.get("routingKey").asText());
        assertEquals(92.0, readingJson.get("vibration").asDouble());
    }
}
