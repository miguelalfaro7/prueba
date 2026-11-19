package com.example.iot.sensorproducer;

import com.example.iot.sensorproducer.dto.SensorReadingEvent;

public interface ReadingPublisher {
    void publish(SensorReadingEvent reading);
}
