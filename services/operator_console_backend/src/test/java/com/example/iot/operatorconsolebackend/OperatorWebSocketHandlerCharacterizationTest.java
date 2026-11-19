package com.example.iot.operatorconsolebackend;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.iot.operatorconsolebackend.dto.RoutedAlert;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class OperatorWebSocketHandlerCharacterizationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void replaysStoredAlertsInOrderAsTheExistingJsonContract() throws Exception {
        PendingAlertStore store = new PendingAlertStore();
        store.addAlert(alert("alert-1"));
        store.addAlert(alert("alert-2"));
        OperatorWebSocketHandler handler = new OperatorWebSocketHandler(store, mapper);
        WebSocketSession session = session("session-1");

        handler.afterConnectionEstablished(session);

        org.mockito.ArgumentCaptor<TextMessage> messages =
                org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.times(2)).sendMessage(messages.capture());
        JsonNode first = mapper.readTree(messages.getAllValues().get(0).getPayload());
        JsonNode second = mapper.readTree(messages.getAllValues().get(1).getPayload());
        assertEquals("alert-1", first.get("alert_id").asText());
        assertEquals("sensor_A", first.get("sensor_id").asText());
        assertEquals("alert-2", second.get("alert_id").asText());
        assertEquals("APAGADO_INMEDIATO", first.get("actions").get(0).asText());
    }

    @Test
    void broadcastsAnIncomingAlertToEveryConnectedSession() throws Exception {
        OperatorWebSocketHandler handler = new OperatorWebSocketHandler(new PendingAlertStore(), mapper);
        WebSocketSession firstSession = session("first");
        WebSocketSession secondSession = session("second");
        handler.afterConnectionEstablished(firstSession);
        handler.afterConnectionEstablished(secondSession);

        handler.broadcastAlert(alert("alert-1"));

        verify(firstSession).sendMessage(any(TextMessage.class));
        verify(secondSession).sendMessage(any(TextMessage.class));
    }

    @Test
    void skipsClosedSessionsAndSwallowsSendFailures() throws Exception {
        OperatorWebSocketHandler handler = new OperatorWebSocketHandler(new PendingAlertStore(), mapper);
        WebSocketSession closedSession = session("closed");
        when(closedSession.isOpen()).thenReturn(false);
        assertDoesNotThrow(() -> handler.afterConnectionEstablished(closedSession));
        handler.broadcastAlert(alert("alert-1"));
        verify(closedSession, never()).sendMessage(any(TextMessage.class));

        PendingAlertStore store = new PendingAlertStore();
        store.addAlert(alert("alert-2"));
        OperatorWebSocketHandler failingHandler = new OperatorWebSocketHandler(store, mapper);
        WebSocketSession failingSession = session("failing");
        when(failingSession.isOpen()).thenReturn(true);
        doThrow(new IOException("connection lost")).when(failingSession).sendMessage(any(TextMessage.class));
        assertDoesNotThrow(() -> failingHandler.afterConnectionEstablished(failingSession));

        assertDoesNotThrow(() -> failingHandler.broadcastAlert(alert("alert-3")));
        verify(failingSession, org.mockito.Mockito.times(1)).sendMessage(any(TextMessage.class));

        WebSocketSession reconnectedSession = session("reconnected");
        assertDoesNotThrow(() -> failingHandler.afterConnectionEstablished(reconnectedSession));
        org.mockito.ArgumentCaptor<TextMessage> replayed =
                org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(reconnectedSession).sendMessage(replayed.capture());
        assertEquals("alert-2", mapper.readTree(replayed.getValue().getPayload())
                .get("alert_id").asText());
    }

    private WebSocketSession session(String id) {
        WebSocketSession session = org.mockito.Mockito.mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private RoutedAlert alert(String id) {
        return new RoutedAlert(id, "sensor_A", "CRITICAL", 91.0,
                "2026-01-01T00:00:00Z", List.of("APAGADO_INMEDIATO"));
    }
}
