package com.example.iot.historyservice.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.iot.historyservice.entity.Alert;
import com.example.iot.historyservice.entity.SensorReading;
import com.example.iot.historyservice.repository.AlertRepository;
import com.example.iot.historyservice.repository.SensorReadingRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoryPersistenceServiceTest {

    @Mock
    private SensorReadingRepository readings;
    @Mock
    private AlertRepository alerts;

    private HistoryPersistenceService history;

    @BeforeEach
    void setUp() {
        history = new HistoryPersistenceService(readings, alerts, 10);
    }

    @Test
    void reportsMissingAndSampledOutReadingsWithoutPersisting() {
        assertEquals(
                HistoryPersistenceService.ReadingResult.MISSING_FIELDS,
                history.storeReading(null, 12.0, "2026-01-01T00:00:00Z")
        );
        assertEquals(
                HistoryPersistenceService.ReadingResult.STORED,
                history.storeReading("sensor_A", 12.0, "2026-01-01T00:00:00Z")
        );
        assertEquals(
                HistoryPersistenceService.ReadingResult.SAMPLED_OUT,
                history.storeReading("sensor_A", 13.0, "2026-01-01T00:00:09Z")
        );

        verify(readings).save(any(SensorReading.class));
    }


    @Test
    void skipsDuplicateAlertsUsingRepositoryExistenceCheck() {
        when(alerts.existsByAlertId("alert-1")).thenReturn(true);

        assertEquals(
                HistoryPersistenceService.AlertResult.DUPLICATE,
                history.storeAlert("sensor_A", 90.0, "alert-1", "2026-01-01T00:00:00Z",
                        "WARNING", "{}")
        );

        verify(alerts, never()).save(any(Alert.class));
    }

    @Test
    void timestampFallbackRemainsCurrentTimeForMissingOrInvalidValues() {
        Instant before = Instant.now();
        history.storeAlert("sensor_A", 90.0, null, "invalid", "WARNING", "{}");
        Instant after = Instant.now();

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alerts).save(captor.capture());
        assertFalse(captor.getValue().getOccurredAt().isBefore(before));
        assertFalse(captor.getValue().getOccurredAt().isAfter(after));
        assertEquals("{}", captor.getValue().getPayload());
    }
}
