package com.example.iot.alertdetector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.iot.alertdetector.dto.SensorAlert;
import com.example.iot.alertdetector.dto.SensorStatusEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class AlertDetectorCharacterizationTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AlertDetector detector;

    @BeforeEach
    void setUp() {
        detector = new AlertDetector(new KafkaSensorEventPublisher(kafkaTemplate), objectMapper,
                com.example.iot.alertdetector.domain.WindowEvaluation.CONSECUTIVE);
    }

    @Test
    void publishesNormalStatusForReadingBelowWarningThreshold() throws Exception {
        detector.processSensorData(readingJson("sensor_A", 75.0));

        SensorStatusEvent status = captureStatus();
        assertEquals("NORMAL", status.status());
        assertEquals(75.0, status.vibration());
        verify(kafkaTemplate, never()).send(eq("alerts_warning"), eq("sensor_A"), any());
        verify(kafkaTemplate, never()).send(eq("alerts_critical"), eq("sensor_A"), any());
    }

    @Test
    void emitsOneWarningAfterThreeConsecutiveValuesStrictlyAbove75() throws Exception {
        detector.processSensorData(readingJson("sensor_A", 75.01));
        detector.processSensorData(readingJson("sensor_A", 80.0));
        detector.processSensorData(readingJson("sensor_A", 90.0));

        assertEquals("WARNING", captureLastStatus().status());
        verify(kafkaTemplate).send(eq("alerts_warning"), eq("sensor_A"), any(SensorAlert.class));
        verify(kafkaTemplate, never()).send(eq("alerts_critical"), eq("sensor_A"), any());
    }

    @Test
    void emitsCriticalOnlyForValuesStrictlyAbove90() throws Exception {
        detector.processSensorData(readingJson("sensor_A", 90.0));
        assertEquals("NORMAL", captureLastStatus().status());
        verify(kafkaTemplate, never()).send(eq("alerts_critical"), eq("sensor_A"), any());

        detector.processSensorData(readingJson("sensor_A", 90.01));

        assertEquals("CRITICAL", captureLastStatus().status());
        verify(kafkaTemplate).send(eq("alerts_critical"), eq("sensor_A"), any(SensorAlert.class));
    }

    @Test
    void warningClearsTheWindowBeforeTheNextSequence() throws Exception {
        detector.processSensorData(readingJson("sensor_A", 80.0));
        detector.processSensorData(readingJson("sensor_A", 81.0));
        detector.processSensorData(readingJson("sensor_A", 82.0));
        detector.processSensorData(readingJson("sensor_A", 83.0));
        detector.processSensorData(readingJson("sensor_A", 84.0));

        verify(kafkaTemplate, times(1))
                .send(eq("alerts_warning"), eq("sensor_A"), any(SensorAlert.class));

        detector.processSensorData(readingJson("sensor_A", 85.0));

        verify(kafkaTemplate, times(2))
                .send(eq("alerts_warning"), eq("sensor_A"), any(SensorAlert.class));
    }

    @Test
    void tracksWindowsIndependentlyPerSensor() throws Exception {
        detector.processSensorData(readingJson("sensor_A", 80.0));
        detector.processSensorData(readingJson("sensor_B", 80.0));
        detector.processSensorData(readingJson("sensor_A", 81.0));
        detector.processSensorData(readingJson("sensor_B", 81.0));
        detector.processSensorData(readingJson("sensor_A", 82.0));

        verify(kafkaTemplate).send(eq("alerts_warning"), eq("sensor_A"), any(SensorAlert.class));
        verify(kafkaTemplate, never()).send(eq("alerts_warning"), eq("sensor_B"), any());
    }

    @Test
    void ignoresReadingWhenVibrationIsMissing() throws Exception {
        detector.processSensorData("""
                {"sensor_id":"sensor_A","timestamp":"2026-01-01T00:00:00Z"}
                """);

        verify(kafkaTemplate, never()).send(any(String.class), any(String.class), any());
    }

    @Test
    void ignoresNonFiniteVibration() {
        detector.processSensorData("""
                {"sensor_id":"sensor_A","vibration":"NaN","timestamp":"2026-01-01T00:00:00Z"}
                """);

        detector.processSensorData("""
                {"sensor_id":"sensor_A","vibration":"Infinity","timestamp":"2026-01-01T00:00:00Z"}
                """);

        verify(kafkaTemplate, never()).send(any(String.class), any(String.class), any());
    }

    @Test
    void ignoresMalformedJsonWithoutPublishing() {
        detector.processSensorData("not-json");

        verify(kafkaTemplate, never()).send(any(String.class), any(String.class), any());
    }

    @Test
    void keepsExistingJsonFieldNamesForPublishedEvents() throws Exception {
        detector.processSensorData(readingJson("sensor_A", 91.0));

        ArgumentCaptor<SensorAlert> alertCaptor = ArgumentCaptor.forClass(SensorAlert.class);
        verify(kafkaTemplate).send(eq("alerts_critical"), eq("sensor_A"), alertCaptor.capture());
        JsonNode alertJson = objectMapper.readTree(objectMapper.writeValueAsString(alertCaptor.getValue()));

        assertEquals("sensor_A", alertJson.get("sensor_id").asText());
        assertEquals("CRITICAL", alertJson.get("type").asText());
        assertTrue(alertJson.hasNonNull("alert_id"));
        assertTrue(alertJson.has("vibration"));
        assertTrue(alertJson.has("timestamp"));
    }

    private SensorStatusEvent captureStatus() throws Exception {
        return captureLastStatus();
    }

    private SensorStatusEvent captureLastStatus() throws Exception {
        ArgumentCaptor<SensorStatusEvent> captor = ArgumentCaptor.forClass(SensorStatusEvent.class);
        verify(kafkaTemplate, org.mockito.Mockito.atLeastOnce())
                .send(eq("sensor_status"), any(String.class), captor.capture());
        List<SensorStatusEvent> statuses = captor.getAllValues();
        return statuses.get(statuses.size() - 1);
    }

    private String readingJson(String sensorId, double vibration) {
        return """
                {"sensor_id":"%s","vibration":%s,"timestamp":"2026-01-01T00:00:00Z"}
                """.formatted(sensorId, vibration);
    }
}
