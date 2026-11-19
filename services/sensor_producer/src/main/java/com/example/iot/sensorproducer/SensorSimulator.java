
package com.example.iot.sensorproducer;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.iot.sensorproducer.dto.SensorReadingEvent;

@Component
public class SensorSimulator {

    private final ReadingPublisher publisher;


    private static final String[] SENSORS = {
        "sensor_A", "sensor_B", "sensor_C", "sensor_D",
        "sensor_E", "sensor_F", "sensor_G", "sensor_H",
        "sensor_I", "sensor_J"
    };

    public SensorSimulator(ReadingPublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(fixedRate = 2000)
    public void generateSensorData() {

        for (String sensorId : SENSORS) {

            double vibration = generateVibration();

            SensorReadingEvent data = new SensorReadingEvent(
                    sensorId,
                    vibration,
                    Instant.now().toString()
            );

            publisher.publish(data);

            System.out.println(
                "[KAFKA] " + sensorId +
                " -> Vibracion: " + vibration
            );
        }
    }

    private double generateVibration() {

        ThreadLocalRandom random = ThreadLocalRandom.current();

        int probability = random.nextInt(100);

        if (probability < 70) {
            return random.nextDouble(20, 70);
        }

        if (probability < 90) {
            return random.nextDouble(76, 90);
        }

        return random.nextDouble(91, 100);
    }
}
