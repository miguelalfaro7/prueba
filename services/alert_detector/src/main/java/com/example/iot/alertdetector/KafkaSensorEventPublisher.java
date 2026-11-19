package com.example.iot.alertdetector;

import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaSensorEventPublisher implements SensorEventPublisher {
    private final KafkaTemplate<String, Object> kafka;

    KafkaSensorEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public CompletableFuture<?> publish(String topic, String sensorId, Object event) {
        return kafka.send(topic, sensorId, event);
    }
}
