package com.example.iot.historyservice.consumer;

import com.example.iot.historyservice.application.HistoryPersistenceService;
import com.example.iot.historyservice.dto.SensorAlertEvent;
import com.example.iot.historyservice.dto.SensorReadingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HistoryKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(HistoryKafkaConsumer.class);

    private final ObjectMapper mapper;
    private final HistoryPersistenceService history;

    public HistoryKafkaConsumer(ObjectMapper mapper, HistoryPersistenceService history) {
        this.mapper = mapper;
        this.history = history;
    }

    @KafkaListener(topics = "sensor_data", groupId = "history-service")
    public void onReading(String json) {
        SensorReadingEvent event;
        try {
            event = mapper.readValue(json, SensorReadingEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializando lectura: {}", json, e);
            return;
        }

        try {
            if (event == null) {
                log.warn("Lectura ignorada, faltan campos: {}", json);
                return;
            }
            HistoryPersistenceService.ReadingResult result = history.storeReading(
                    event.sensorId(), event.vibration(), event.timestamp());
            if (result == HistoryPersistenceService.ReadingResult.MISSING_FIELDS) {
                log.warn("Lectura ignorada, faltan campos: {}", json);
            }
        } catch (DataAccessException e) {
            log.error("Error de base de datos guardando lectura: {}", json, e);
        } catch (Exception e) {
            log.error("Error inesperado guardando lectura: {}", json, e);
        }
    }

    @KafkaListener(topics = "alerts_critical", groupId = "history-service")
    public void onCritical(String json) {
        saveAlert(json, "CRITICAL");
    }

    @KafkaListener(topics = "alerts_warning", groupId = "history-service")
    public void onWarning(String json) {
        saveAlert(json, "WARNING");
    }

    private void saveAlert(String json, String severity) {
        SensorAlertEvent event;
        try {
            event = mapper.readValue(json, SensorAlertEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializando alerta {}: {}", severity, json, e);
            return;
        }

        try {
            if (event == null) {
                log.error("Error deserializando alerta {}: mensaje JSON nulo", severity);
                return;
            }
            history.storeAlert(
                    event.sensorId(), event.vibration(), event.alertId(),
                    event.timestamp(), severity, json);
        } catch (DataAccessException e) {
            log.error("Error de base de datos guardando alerta {}: {}", severity, json, e);
        } catch (Exception e) {
            log.error("Error inesperado guardando alerta {}: {}", severity, json, e);
        }
    }
}
