package com.example.iot.alertrouter.domain;

import java.util.List;
import java.util.Map;

public final class AlertRoutingPolicy {

    private static final Map<AlertSeverity, List<AlertAction>> ACTIONS_BY_SEVERITY = Map.of(
            AlertSeverity.CRITICAL,
            List.of(AlertAction.SHUT_DOWN_IMMEDIATELY, AlertAction.IGNORE_FOR_10_MINUTES),
            AlertSeverity.WARNING,
            List.of(AlertAction.SCHEDULE_MAINTENANCE_NOW, AlertAction.ACKNOWLEDGE_AND_WAIT_24_HOURS)
    );

    public RoutingDecision decide(String rawSeverity) {
        AlertSeverity severity = AlertSeverity.from(rawSeverity);
        return new RoutingDecision(severity, ACTIONS_BY_SEVERITY.get(severity));
    }
}
