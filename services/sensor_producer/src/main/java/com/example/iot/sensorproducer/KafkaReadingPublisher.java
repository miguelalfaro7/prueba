package com.example.iot.sensorproducer;

import com.example.iot.sensorproducer.dto.SensorReadingEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaReadingPublisher implements ReadingPublisher {
    private final KafkaTemplate<String, Object> kafka;

    KafkaReadingPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(SensorReadingEvent reading) {
        kafka.send("sensor_data", reading.sensorId(), reading);
    }
}
