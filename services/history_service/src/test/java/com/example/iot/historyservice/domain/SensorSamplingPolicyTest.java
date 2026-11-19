package com.example.iot.historyservice.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SensorSamplingPolicyTest {

    @Test
    void appliesConfiguredIntervalPerSensorAndAcceptsTheExactBoundary() {
        SensorSamplingPolicy policy = new SensorSamplingPolicy(10);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        assertTrue(policy.shouldStore("sensor_A", start));
        assertFalse(policy.shouldStore("sensor_A", start.plusSeconds(9)));
        assertTrue(policy.shouldStore("sensor_B", start.plusSeconds(1)));
        assertTrue(policy.shouldStore("sensor_A", start.plusSeconds(10)));
    }
}
