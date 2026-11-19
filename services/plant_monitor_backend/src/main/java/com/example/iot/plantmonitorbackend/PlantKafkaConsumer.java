package com.example.iot.plantmonitorbackend;

import com.example.iot.plantmonitorbackend.dto.SensorAlert;
import com.example.iot.plantmonitorbackend.dto.SensorStatusEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PlantKafkaConsumer {

    private final PlantWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public PlantKafkaConsumer(
            PlantWebSocketHandler webSocketHandler,
            ObjectMapper objectMapper) {
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "sensor_status",
            groupId = "plant-monitor-status-group"
    )
    public void receiveSensorStatus(String message) throws JsonProcessingException {

        System.out.println(
                "[KAFKA] Estado de sensor recibido: " + message
        );

        SensorStatusEvent status = objectMapper.readValue(message, SensorStatusEvent.class);
        webSocketHandler.updateStatus(status.sensorId(), objectMapper.writeValueAsString(status));
    }

    @KafkaListener(
            topics = {"alerts_critical", "alerts_warning"},
            groupId = "plant-monitor-alerts-group"
    )
    public void receiveSensorAlert(String message) throws JsonProcessingException {

        System.out.println(
                "[KAFKA] Alerta de sensor recibida: " + message
        );

        SensorAlert alert = objectMapper.readValue(message, SensorAlert.class);
        webSocketHandler.broadcastStatus(objectMapper.writeValueAsString(alert));
    }
}
