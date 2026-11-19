package com.example.iot.operatorconsolebackend;

import org.springframework.stereotype.Component;
import com.example.iot.operatorconsolebackend.dto.RoutedAlert;

import java.util.ArrayList;
import java.util.List;

@Component
public class PendingAlertStore {

    private final List<RoutedAlert> pendingAlerts = new ArrayList<>();

    public synchronized void addAlert(RoutedAlert alert) {
        pendingAlerts.add(alert);
    }

    public synchronized List<RoutedAlert> getPendingAlerts() {
        return new ArrayList<>(pendingAlerts);
    }
}
