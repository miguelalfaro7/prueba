package com.example.iot.historyservice.consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.iot.historyservice.entity.Alert;
import com.example.iot.historyservice.entity.SensorReading;
import com.example.iot.historyservice.application.HistoryPersistenceService;
import com.example.iot.historyservice.repository.AlertRepository;
import com.example.iot.historyservice.repository.SensorReadingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoryKafkaConsumerCharacterizationTest {

    @Mock
    private SensorReadingRepository readings;
    @Mock
    private AlertRepository alerts;
    private HistoryKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        HistoryPersistenceService history = new HistoryPersistenceService(readings, alerts, 10);
        consumer = new HistoryKafkaConsumer(new ObjectMapper(), history);
    }

    @Test
    void samplesAtConfiguredBoundaryAndKeepsSensorWindowsIndependent() {
        consumer.onReading(reading("sensor_A", "2026-01-01T00:00:00Z"));
        consumer.onReading(reading("sensor_A", "2026-01-01T00:00:09Z"));
        consumer.onReading(reading("sensor_B", "2026-01-01T00:00:01Z"));
        consumer.onReading(reading("sensor_A", "2026-01-01T00:00:10Z"));

        ArgumentCaptor<SensorReading> captor = ArgumentCaptor.forClass(SensorReading.class);
        verify(readings, times(3)).save(captor.capture());
        assertEquals("sensor_A", captor.getAllValues().get(0).getSensorId());
        assertEquals("sensor_B", captor.getAllValues().get(1).getSensorId());
        assertEquals(Instant.parse("2026-01-01T00:00:10Z"), captor.getAllValues().get(2).getRecordedAt());
    }

    @Test
    void updatesSamplingTimestampBeforePersistenceSucceeds() {
        doThrow(new IllegalStateException("database unavailable"))
                .when(readings)
                .save(any(SensorReading.class));

        consumer.onReading(reading("sensor_A", "2026-01-01T00:00:00Z"));
        consumer.onReading(reading("sensor_A", "2026-01-01T00:00:05Z"));

        verify(readings, times(1)).save(any(SensorReading.class));
    }

    @Test
    void missingReadingFieldsAreIgnoredAndInvalidTimestampFallsBackToCurrentTime() {
        consumer.onReading("""
                {"sensor_id":"sensor_A"}
                """);
        verify(readings, never()).save(any(SensorReading.class));

        Instant before = Instant.now();
        consumer.onReading("""
                {"sensor_id":"sensor_B","vibration":4.5,"timestamp":"invalid"}
                """);
        Instant after = Instant.now();

        ArgumentCaptor<SensorReading> captor = ArgumentCaptor.forClass(SensorReading.class);
        verify(readings).save(captor.capture());
        assertFalse(captor.getValue().getRecordedAt().isBefore(before));
        assertFalse(captor.getValue().getRecordedAt().isAfter(after));
    }

    @Test
    void deduplicatesAlertsOnlyWhenAlertIdExistsAndUsesUnknownSensorFallback() {
        when(alerts.existsByAlertId("duplicate-id")).thenReturn(true);
        consumer.onCritical(alertJson("duplicate-id", "sensor_A", "2026-01-01T00:00:00Z"));

        verify(alerts, never()).save(any(Alert.class));
        consumer.onWarning(alertJson(null, null, "2026-01-01T00:00:00Z"));

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alerts).save(captor.capture());
        assertEquals("UNKNOWN", captor.getValue().getSensorId());
        assertEquals("WARNING", captor.getValue().getSeverity());
        assertTrue(captor.getValue().getPayload().contains("\"type\":\"WARNING\""));
        verify(alerts, never()).existsByAlertId(null);
    }

    @Test
    void invalidJsonAndPersistenceFailuresAreSwallowed() {
        assertDoesNotThrow(() -> consumer.onReading("invalid-json"));
        doThrow(new IllegalStateException("database unavailable"))
                .when(readings)
                .save(any(SensorReading.class));
        assertDoesNotThrow(() -> consumer.onReading(reading("sensor_A", "2026-01-01T00:00:00Z")));
    }

    private String reading(String sensorId, String timestamp) {
        return """
                {"sensor_id":"%s","vibration":12.5,"timestamp":"%s"}
                """.formatted(sensorId, timestamp);
    }

    private String alertJson(String alertId, String sensorId, String timestamp) {
        return """
                {"alert_id":%s,"sensor_id":%s,"type":"WARNING","vibration":4.5,"timestamp":"%s"}
                """.formatted(
                alertId == null ? "null" : "\"" + alertId + "\"",
                sensorId == null ? "null" : "\"" + sensorId + "\"",
                timestamp
        );
    }
}
