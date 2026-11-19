package com.example.iot.alertdetector.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SensorStateTest {

    @Test
    void classifiesNormalValuesAndStrictThresholdBoundaries() {
        SensorState state = new SensorState("sensor_A");

        assertEquals(SensorClassification.NORMAL, state.process(reading("sensor_A", 75.0)));
        assertEquals(SensorClassification.NORMAL, state.process(reading("sensor_A", 75.01)));
        assertEquals(SensorClassification.NORMAL, state.process(reading("sensor_A", 90.0)));
        assertEquals(SensorClassification.CRITICAL, state.process(reading("sensor_A", 90.01)));
    }

    @Test
    void emitsWarningAfterThreeConsecutiveValuesAndClearsAfterPublicationCompletes() {
        SensorState state = new SensorState("sensor_A");

        assertEquals(SensorClassification.NORMAL, state.process(reading("sensor_A", 80.0)));
        assertEquals(SensorClassification.NORMAL, state.process(reading("sensor_A", 81.0)));
        assertEquals(SensorClassification.WARNING, state.process(reading("sensor_A", 82.0)));
        assertEquals(SensorClassification.WARNING, state.process(reading("sensor_A", 83.0)));
        state.completeWarningPublication();
        assertEquals(SensorClassification.NORMAL, state.process(reading("sensor_A", 83.0)));
        assertEquals(SensorClassification.NORMAL, state.process(reading("sensor_A", 84.0)));
        assertEquals(SensorClassification.WARNING, state.process(reading("sensor_A", 85.0)));
    }

    @Test
    void criticalClassificationHasPriorityAndLeavesTheWindowIntact() {
        SensorState state = new SensorState("sensor_A");

        assertEquals(SensorClassification.NORMAL, state.process(reading("sensor_A", 80.0)));
        assertEquals(SensorClassification.NORMAL, state.process(reading("sensor_A", 81.0)));
        assertEquals(SensorClassification.CRITICAL, state.process(reading("sensor_A", 91.0)));
        assertEquals(SensorClassification.WARNING, state.process(reading("sensor_A", 82.0)));
    }

    @Test
    void rejectsMismatchedSensorIdentity() {
        SensorState state = new SensorState("sensor_A");

        assertThrows(
                IllegalArgumentException.class,
                () -> state.process(reading("sensor_B", 50.0))
        );
    }

    @Test
    void readingRejectsMissingIdentityOrVibrationAndNonFiniteValues() {
        assertThrows(IllegalArgumentException.class, () -> new SensorReading(" ", 10.0));
        assertThrows(IllegalArgumentException.class, () -> new SensorReading("sensor_A", null));
        assertThrows(IllegalArgumentException.class, () -> new SensorReading("sensor_A", Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new SensorReading("sensor_A", Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new SensorReading("sensor_A", Double.NEGATIVE_INFINITY));
    }

    @Test
    void windowEvaluationCanBeChangedWithoutChangingCriticalPriority() {
        SensorState average = new SensorState("sensor_A", WindowEvaluation.SIMPLE_AVERAGE);
        SensorState weighted = new SensorState("sensor_A", WindowEvaluation.exponentialAverage(0.8));
        for (SensorState state : java.util.List.of(average, weighted)) {
            state.process(reading("sensor_A", 70.0));
            state.process(reading("sensor_A", 80.0));
            assertEquals(SensorClassification.WARNING, state.process(reading("sensor_A", 85.0)));
            assertEquals(SensorClassification.CRITICAL, state.process(reading("sensor_A", 95.0)));
        }
    }

    private SensorReading reading(String sensorId, double vibration) {
        return new SensorReading(sensorId, vibration);
    }
}
