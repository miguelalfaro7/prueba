package com.example.iot.operatorconsolebackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.example.iot.operatorconsolebackend.dto.RoutedAlert;
import com.example.iot.operatorconsolebackend.application.AlertBroadcaster;
import com.example.iot.operatorconsolebackend.application.OperatorAlertApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class RabbitMQAlertListenerCharacterizationTest {

    @Test
    void deserializesTheExistingRabbitContractStoresThenBroadcasts() throws Exception {
        AlertBroadcaster broadcaster = mock(AlertBroadcaster.class);
        PendingAlertStore store = mock(PendingAlertStore.class);
        OperatorAlertApplicationService alertService =
                new OperatorAlertApplicationService(store, broadcaster);
        RabbitMQAlertListener listener = new RabbitMQAlertListener(alertService, new ObjectMapper());
        String body = """
                {"alert_id":"alert-1","sensor_id":"sensor_A","type":"CRITICAL","vibration":91.5,"timestamp":"2026-01-01T00:00:00Z","actions":["APAGADO_INMEDIATO"]}
                """;

        listener.receiveAlert(body);

        InOrder order = inOrder(store, broadcaster);
        order.verify(store).addAlert(org.mockito.ArgumentMatchers.any(RoutedAlert.class));
        order.verify(broadcaster).broadcastAlert(org.mockito.ArgumentMatchers.any(RoutedAlert.class));
        RoutedAlert alert = new ObjectMapper().readValue(body, RoutedAlert.class);
        assertEquals("alert-1", alert.alertId());
        assertEquals(List.of("APAGADO_INMEDIATO"), alert.actions());
    }
}
