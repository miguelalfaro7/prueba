package com.example.iot.alertrouter.domain;

import java.util.List;
import java.util.Objects;

public record RoutingDecision(AlertSeverity severity, List<AlertAction> actions) {

    public RoutingDecision {
        Objects.requireNonNull(severity, "La severidad es obligatoria");
        actions = List.copyOf(actions);
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("La decisión requiere al menos una acción");
        }
    }
}
