
package com.example.iot.alertdetector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.iot.alertdetector.domain.SensorClassification;
import com.example.iot.alertdetector.domain.SensorReading;
import com.example.iot.alertdetector.domain.SensorState;
import com.example.iot.alertdetector.dto.SensorAlert;
import com.example.iot.alertdetector.dto.SensorReadingEvent;
import com.example.iot.alertdetector.dto.SensorStatusEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import com.example.iot.alertdetector.domain.AlertId;
import com.example.iot.alertdetector.domain.WindowEvaluation;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AlertDetector {

    private static final Logger log = LoggerFactory.getLogger(AlertDetector.class);
    private static final String SENSOR_STATUS_TOPIC = "sensor_status";
    private static final String CRITICAL_ALERTS_TOPIC = "alerts_critical";
    private static final String WARNING_ALERTS_TOPIC = "alerts_warning";

    private final SensorEventPublisher events;
    private final WindowEvaluation evaluation;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, SensorState> sensorStates =
            new ConcurrentHashMap<>();

    public AlertDetector(
            SensorEventPublisher events,
            ObjectMapper objectMapper, WindowEvaluation evaluation) {

        this.events = events;
        this.evaluation = evaluation;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "sensor_data",
            groupId = "alert-detector-group"
    )
    public void processSensorData(String message) {

        try {
            SensorReadingEvent event = objectMapper.readValue(
                    message,
                    SensorReadingEvent.class
            );
            if (event == null) {
                throw new IllegalArgumentException("El evento de lectura es obligatorio");
            }

            SensorReading reading = new SensorReading(
                    event.sensorId(),
                    event.vibration()
            );
            SensorState state = sensorStates.computeIfAbsent(
                    reading.sensorId(),
                    sensorId -> new SensorState(sensorId, evaluation)
            );

            synchronized (state) {
                SensorClassification classification = state.process(reading);

                log.info(
                        "[DETECTOR] {} -> Vibracion: {}",
                        reading.sensorId(),
                        reading.vibration()
                );

                SensorStatusEvent sensorStatus = new SensorStatusEvent(
                        reading.sensorId(),
                        reading.vibration(),
                        classification.name(),
                        Instant.now().toString()
                );

                publish(SENSOR_STATUS_TOPIC, reading.sensorId(), sensorStatus);

                if (classification != SensorClassification.NORMAL) {
                    publishAlert(reading, classification);
                }

                if (classification == SensorClassification.WARNING) {
                    state.completeWarningPublication();
                }
            }

        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[DETECTOR] Mensaje de sensor invalido; se ignora: {}", e.getMessage());
        } catch (Exception e) {
            // Mantiene el manejo actual del listener: registrar el fallo sin cambiar el ack.
            log.error("[DETECTOR] Error interno procesando mensaje de sensor", e);
        }
    }

    private void publishAlert(SensorReading reading, SensorClassification classification) {

        String topic = switch (classification) {
            case CRITICAL -> CRITICAL_ALERTS_TOPIC;
            case WARNING -> WARNING_ALERTS_TOPIC;
            case NORMAL -> throw new IllegalArgumentException(
                    "Una lectura normal no genera alerta"
            );
        };

        SensorAlert alert = new SensorAlert(
                AlertId.create().toString(),
                reading.sensorId(),
                classification.name(),
                reading.vibration(),
                Instant.now().toString()
        );

        publish(topic, reading.sensorId(), alert);

        log.info(
                "[ALERTA {}] {} -> {}",
                classification,
                reading.sensorId(),
                reading.vibration()
        );
    }

    private void publish(String topic, String sensorId, Object event) {
        CompletableFuture<?> result = events.publish(topic, sensorId, event);
        if (result != null) {
            result.whenComplete((sendResult, error) -> {
                if (error != null) {
                    log.error(
                            "[KAFKA] Error publicando en topic={} sensorId={}",
                            topic,
                            sensorId,
                            error
                    );
                }
            });
        }
    }
}
