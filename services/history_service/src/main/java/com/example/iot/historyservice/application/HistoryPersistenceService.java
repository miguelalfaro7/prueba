package com.example.iot.historyservice.application;

import com.example.iot.historyservice.domain.AlertDeduplicationPolicy;
import com.example.iot.historyservice.domain.SensorSamplingPolicy;
import com.example.iot.historyservice.entity.Alert;
import com.example.iot.historyservice.entity.SensorReading;
import com.example.iot.historyservice.repository.AlertRepository;
import com.example.iot.historyservice.repository.SensorReadingRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HistoryPersistenceService {

    private final SensorReadingRepository readings;
    private final AlertRepository alerts;
    private final SensorSamplingPolicy samplingPolicy;
    private final AlertDeduplicationPolicy deduplicationPolicy = new AlertDeduplicationPolicy();

    public HistoryPersistenceService(
            SensorReadingRepository readings,
            AlertRepository alerts,
            @Value("${history.readings.sample-seconds:10}") long sampleSeconds) {
        this.readings = readings;
        this.alerts = alerts;
        this.samplingPolicy = new SensorSamplingPolicy(sampleSeconds);
    }

    public ReadingResult storeReading(String sensorId, Double vibration, String timestamp) {
        if (sensorId == null || vibration == null) {
            return ReadingResult.MISSING_FIELDS;
        }

        Instant recordedAt = parseInstant(timestamp);
        if (!samplingPolicy.shouldStore(sensorId, recordedAt)) {
            return ReadingResult.SAMPLED_OUT;
        }

        readings.save(new SensorReading(sensorId, vibration, recordedAt));
        return ReadingResult.STORED;
    }

    public AlertResult storeAlert(
            String sensorId,
            Double vibration,
            String alertId,
            String timestamp,
            String severity,
            String originalPayload) {
        boolean alertIdAlreadyExists = alertId != null && alerts.existsByAlertId(alertId);
        if (deduplicationPolicy.isDuplicate(alertId, alertIdAlreadyExists)) {
            return AlertResult.DUPLICATE;
        }

        Instant occurredAt = parseInstant(timestamp);
        alerts.save(new Alert(
                sensorId != null ? sensorId : "UNKNOWN",
                severity,
                vibration,
                originalPayload,
                alertId,
                occurredAt
        ));
        return AlertResult.STORED;
    }

    private Instant parseInstant(String text) {
        try {
            return text != null ? Instant.parse(text) : Instant.now();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    public enum ReadingResult {
        STORED,
        SAMPLED_OUT,
        MISSING_FIELDS
    }

    public enum AlertResult {
        STORED,
        DUPLICATE
    }
}
