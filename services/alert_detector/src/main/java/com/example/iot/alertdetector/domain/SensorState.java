package com.example.iot.alertdetector.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class SensorState {
    private static final int WINDOW_SIZE = 3;
    private static final double WARNING_THRESHOLD = 75.0;
    private static final double CRITICAL_THRESHOLD = 90.0;

    private final SensorId sensorId;
    private final WindowEvaluation evaluation;
    private final Deque<Double> readings = new ArrayDeque<>(WINDOW_SIZE);
    private SeverityState severity = SeverityState.NORMAL;
    private boolean warningPending;

    public SensorState(String sensorId) {
        this(sensorId, WindowEvaluation.CONSECUTIVE);
    }

    public SensorState(String sensorId, WindowEvaluation evaluation) {
        this.sensorId = new SensorId(sensorId);
        this.evaluation = Objects.requireNonNull(evaluation);
    }

    public synchronized SensorClassification process(SensorReading reading) {
        if (reading == null || !sensorId.value().equals(reading.sensorId())) {
            throw new IllegalArgumentException("La lectura debe pertenecer al sensor");
        }
        readings.addLast(reading.vibration());
        if (readings.size() > WINDOW_SIZE) {
            readings.removeFirst();
        }
        severity = SeverityState.evaluate(reading.vibration() > CRITICAL_THRESHOLD,
                () -> evaluation.exceeds(List.copyOf(readings), WINDOW_SIZE, WARNING_THRESHOLD));
        return severity.enter(this);
    }

    public synchronized void completeWarningPublication() {
        if (warningPending) {
            readings.clear();
            warningPending = false;
        }
    }

    private enum SeverityState {
        NORMAL {
            @Override SensorClassification enter(SensorState sensor) {
                return SensorClassification.NORMAL;
            }
        },
        WARNING {
            @Override SensorClassification enter(SensorState sensor) {
                sensor.warningPending = true;
                return SensorClassification.WARNING;
            }
        },
        CRITICAL {
            @Override SensorClassification enter(SensorState sensor) {
                return SensorClassification.CRITICAL;
            }
        };

        static SeverityState evaluate(boolean critical, java.util.function.BooleanSupplier warning) {
            if (critical) {
                return CRITICAL;
            }
            return warning.getAsBoolean() ? WARNING : NORMAL;
        }

        abstract SensorClassification enter(SensorState sensor);
    }
}
