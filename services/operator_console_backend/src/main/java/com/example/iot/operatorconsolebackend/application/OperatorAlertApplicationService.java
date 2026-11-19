package com.example.iot.operatorconsolebackend.application;

import com.example.iot.operatorconsolebackend.PendingAlertStore;
import com.example.iot.operatorconsolebackend.dto.RoutedAlert;
import org.springframework.stereotype.Service;

@Service
public class OperatorAlertApplicationService {

    private final PendingAlertStore pendingAlerts;
    private final AlertBroadcaster broadcaster;

    public OperatorAlertApplicationService(
            PendingAlertStore pendingAlerts,
            AlertBroadcaster broadcaster) {
        this.pendingAlerts = pendingAlerts;
        this.broadcaster = broadcaster;
    }

    public void receive(RoutedAlert alert) {
        pendingAlerts.addAlert(alert);
        broadcaster.broadcastAlert(alert);
    }
}
