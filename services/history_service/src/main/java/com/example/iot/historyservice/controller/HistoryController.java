package com.example.iot.historyservice.controller;

import com.example.iot.historyservice.dto.AlertResponse;
import com.example.iot.historyservice.dto.OperatorDecisionResponse;
import com.example.iot.historyservice.dto.SensorReadingResponse;
import com.example.iot.historyservice.repository.AlertRepository;
import com.example.iot.historyservice.repository.OperatorDecisionRepository;
import com.example.iot.historyservice.repository.SensorReadingRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/history")
@CrossOrigin(origins = "*")
public class HistoryController {

    private final AlertRepository alerts;
    private final OperatorDecisionRepository decisions;
    private final SensorReadingRepository readings;

    public HistoryController(AlertRepository alerts,
                             OperatorDecisionRepository decisions,
                             SensorReadingRepository readings) {
        this.alerts = alerts;
        this.decisions = decisions;
        this.readings = readings;
    }

    @GetMapping("/alerts")
    public List<AlertResponse> alerts(@RequestParam(required = false) String sensorId,
                                      @RequestParam(defaultValue = "50") int limit) {
        Pageable page = PageRequest.of(0, clamp(limit));
        return (sensorId == null
                ? alerts.findAllByOrderByCreatedAtDesc(page)
                : alerts.findBySensorIdOrderByCreatedAtDesc(sensorId, page))
                .stream()
                .map(alert -> new AlertResponse(
                        alert.getId(), alert.getAlertId(), alert.getSensorId(), alert.getSeverity(),
                        alert.getVibrationValue(), alert.getOccurredAt(), alert.getPayload(),
                        alert.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @GetMapping("/decisions")
    public List<OperatorDecisionResponse> decisions(@RequestParam(required = false) String sensorId,
                                                    @RequestParam(defaultValue = "50") int limit) {
        Pageable page = PageRequest.of(0, clamp(limit));
        return (sensorId == null
                ? decisions.findAllByOrderByDecidedAtDesc(page)
                : decisions.findBySensorIdOrderByDecidedAtDesc(sensorId, page))
                .stream()
                .map(decision -> new OperatorDecisionResponse(
                        decision.getId(), decision.getAlertId(), decision.getSensorId(),
                        decision.getAction(), decision.getRoutingKey(), decision.getPayload(),
                        decision.getDecidedAt()))
                .collect(Collectors.toList());
    }

    @GetMapping("/readings")
    public List<SensorReadingResponse> readings(@RequestParam(required = false) String sensorId,
                                                @RequestParam(defaultValue = "50") int limit) {
        Pageable page = PageRequest.of(0, clamp(limit));
        return (sensorId == null
                ? readings.findAllByOrderByRecordedAtDesc(page)
                : readings.findBySensorIdOrderByRecordedAtDesc(sensorId, page))
                .stream()
                .map(reading -> new SensorReadingResponse(
                        reading.getId(), reading.getSensorId(), reading.getVibration(),
                        reading.getRecordedAt(), reading.getStoredAt()))
                .collect(Collectors.toList());
    }

    private int clamp(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }
}
