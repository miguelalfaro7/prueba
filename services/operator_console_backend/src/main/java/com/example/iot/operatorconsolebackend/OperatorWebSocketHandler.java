package com.example.iot.operatorconsolebackend;

import com.example.iot.operatorconsolebackend.dto.RoutedAlert;
import com.example.iot.operatorconsolebackend.application.AlertBroadcaster;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OperatorWebSocketHandler extends TextWebSocketHandler implements AlertBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(OperatorWebSocketHandler.class);

    private final Set<WebSocketSession> sessions =
            ConcurrentHashMap.newKeySet();

    private final PendingAlertStore pendingAlertStore;
    private final ObjectMapper objectMapper;

    public OperatorWebSocketHandler(
            PendingAlertStore pendingAlertStore,
            ObjectMapper objectMapper) {
        this.pendingAlertStore = pendingAlertStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {

        sessions.add(session);

        log.info("[WEBSOCKET] Operador conectado: {}", session.getId());

        // Recuperar alertas almacenadas
        List<RoutedAlert> pendingAlerts =
                pendingAlertStore.getPendingAlerts();

        for (RoutedAlert alert : pendingAlerts) {
            sendAlert(session, alert);
        }

        log.info("[WEBSOCKET] Alertas pendientes enviadas: {}", pendingAlerts.size());
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessions.remove(session);

        log.info("[WEBSOCKET] Operador desconectado: {}", session.getId());
    }

    @Override
    public void broadcastAlert(RoutedAlert alert) {

        for (WebSocketSession session : sessions) {
            sendAlert(session, alert);
        }
    }

    private void sendAlert(WebSocketSession session, RoutedAlert alert) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(alert);
        } catch (JsonProcessingException e) {
            log.error("[WEBSOCKET] No se pudo serializar la alerta {}", alert.alertId(), e);
            return;
        }

        if (!session.isOpen()) {
            sessions.remove(session);
            return;
        }

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException e) {
            sessions.remove(session);
            log.warn("[WEBSOCKET] Se retira sesión tras fallar el envío: {}", session.getId(), e);
        }
    }
}
