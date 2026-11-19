package com.example.iot.operatorconsolebackend.application;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.example.iot.operatorconsolebackend.PendingAlertStore;
import com.example.iot.operatorconsolebackend.dto.RoutedAlert;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class OperatorAlertApplicationServiceTest {

    @Test
    void recordsTheAlertBeforeBroadcastingIt() {
        PendingAlertStore store = mock(PendingAlertStore.class);
        AlertBroadcaster broadcaster = mock(AlertBroadcaster.class);
        OperatorAlertApplicationService service = new OperatorAlertApplicationService(store, broadcaster);
        RoutedAlert alert = new RoutedAlert("alert-1", "sensor_A", "CRITICAL", 91.0,
                "2026-01-01T00:00:00Z", List.of("APAGADO_INMEDIATO"));

        service.receive(alert);

        InOrder order = inOrder(store, broadcaster);
        order.verify(store).addAlert(alert);
        order.verify(broadcaster).broadcastAlert(alert);
    }
}
