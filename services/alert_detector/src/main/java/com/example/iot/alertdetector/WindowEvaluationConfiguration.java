package com.example.iot.alertdetector;

import com.example.iot.alertdetector.domain.WindowEvaluation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class WindowEvaluationConfiguration {
    @Bean
    WindowEvaluation windowEvaluation(
            @Value("${iot.detector.warning-strategy:consecutive}") String strategy,
            @Value("${iot.detector.warning-alpha:0.5}") double alpha) {
        return switch (strategy) {
            case "consecutive" -> WindowEvaluation.CONSECUTIVE;
            case "simple-average" -> WindowEvaluation.SIMPLE_AVERAGE;
            case "exponential-average" -> WindowEvaluation.exponentialAverage(alpha);
            default -> throw new IllegalArgumentException("Estrategia de advertencia desconocida: " + strategy);
        };
    }
}
