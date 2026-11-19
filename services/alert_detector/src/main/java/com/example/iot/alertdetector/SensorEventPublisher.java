package com.example.iot.alertdetector;

import java.util.concurrent.CompletableFuture;

public interface SensorEventPublisher {
    CompletableFuture<?> publish(String topic, String sensorId, Object event);
}
