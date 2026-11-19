package com.example.iot.alertdetector.domain;

import java.util.Collection;

@FunctionalInterface
public interface WindowEvaluation {
    boolean exceeds(Collection<Double> readings, int windowSize, double threshold);

    WindowEvaluation CONSECUTIVE = (readings, size, threshold) ->
            readings.size() == size && readings.stream().allMatch(value -> value > threshold);

    WindowEvaluation SIMPLE_AVERAGE = (readings, size, threshold) ->
            readings.size() == size
                    && readings.stream().mapToDouble(Double::doubleValue).average().orElseThrow() > threshold;

    static WindowEvaluation exponentialAverage(double alpha) {
        if (!(alpha > 0 && alpha <= 1)) {
            throw new IllegalArgumentException("alpha debe pertenecer a (0, 1]");
        }
        return (readings, size, threshold) -> {
            if (readings.size() != size || readings.isEmpty()) {
                return false;
            }
            var values = readings.iterator();
            double average = values.next();
            while (values.hasNext()) {
                average += alpha * (values.next() - average);
            }
            return average > threshold;
        };
    }
}
