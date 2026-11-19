package com.example.iot.alertrouter.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertRoutingPolicyTest {

    private final AlertRoutingPolicy policy = new AlertRoutingPolicy();

    @Test
    void criticalSeverityProducesCriticalActionsInExistingOrder() {
        RoutingDecision decision = policy.decide("CRITICAL");

        assertEquals(AlertSeverity.CRITICAL, decision.severity());
        assertEquals(
                List.of(AlertAction.SHUT_DOWN_IMMEDIATELY, AlertAction.IGNORE_FOR_10_MINUTES),
                decision.actions()
        );
        assertEquals(
                List.of("APAGADO_INMEDIATO", "IGNORAR_10_MINUTOS"),
                decision.actions().stream().map(AlertAction::externalName).toList()
        );
    }

    @Test
    void warningSeverityProducesWarningActionsInExistingOrder() {
        RoutingDecision decision = policy.decide("WARNING");

        assertEquals(AlertSeverity.WARNING, decision.severity());
        assertEquals(
                List.of(
                        AlertAction.SCHEDULE_MAINTENANCE_NOW,
                        AlertAction.ACKNOWLEDGE_AND_WAIT_24_HOURS
                ),
                decision.actions()
        );
        assertEquals(
                List.of("PROGRAMAR_MANTENIMIENTO_AHORA", "RECONOCER_Y_ESPERAR_24H"),
                decision.actions().stream().map(AlertAction::externalName).toList()
        );
    }

    @Test
    void rejectsUnknownOrMissingSeverity() {
        assertThrows(IllegalArgumentException.class, () -> policy.decide("UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> policy.decide(null));
    }

    @Test
    void routingDecisionCopiesItsActionList() {
        List<AlertAction> mutableActions = new ArrayList<>(
                List.of(AlertAction.SHUT_DOWN_IMMEDIATELY)
        );

        RoutingDecision decision = new RoutingDecision(AlertSeverity.CRITICAL, mutableActions);
        mutableActions.clear();

        assertEquals(List.of(AlertAction.SHUT_DOWN_IMMEDIATELY), decision.actions());
        assertThrows(
                UnsupportedOperationException.class,
                () -> decision.actions().add(AlertAction.IGNORE_FOR_10_MINUTES)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RoutingDecision(AlertSeverity.CRITICAL, List.of())
        );
    }
}
