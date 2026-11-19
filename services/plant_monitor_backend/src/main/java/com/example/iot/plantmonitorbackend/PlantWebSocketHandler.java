package com.example.iot.plantmonitorbackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Component
public class PlantWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PlantWebSocketHandler.class);
    private static final long REPORT_INTERVAL_NANOS =
            TimeUnit.SECONDS.toNanos(30);

    private final java.util.Map<String, String> latestStatus = new java.util.TreeMap<>();

    private final Set<WebSocketSession> sessions =
            ConcurrentHashMap.newKeySet();
    private final LongAdder broadcasts = new LongAdder();
    private final LongAdder attemptedSends = new LongAdder();
    private final LongAdder successfulSends = new LongAdder();
    private final LongAdder failedSends = new LongAdder();
    private final LongAdder closedSessionsSkipped = new LongAdder();
    private final AtomicLong lastReportNanos = new AtomicLong();
    private final AtomicReference<String> lastSendError = new AtomicReference<>("none");

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws IOException {
        for (String status : latestStatus.values()) {
            session.sendMessage(new TextMessage(status));
        }

        sessions.add(session);

        LOGGER.info(
                "[WEBSOCKET] Dashboard conectado: sessionId={}, activeSessions={}",
                session.getId(),
                sessions.size()
        );
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessions.remove(session);

        LOGGER.info(
                "[WEBSOCKET] Dashboard desconectado: sessionId={}, activeSessions={}, closeStatus={}",
                session.getId(),
                sessions.size(),
                status
        );
    }

    public synchronized void updateStatus(String sensorId, String statusJson) {
        latestStatus.put(sensorId, statusJson);
        broadcastStatus(statusJson);
    }

    public synchronized void broadcastStatus(String statusJson) {

        broadcasts.increment();

        for (WebSocketSession session : sessions) {

            if (session.isOpen()) {

                try {
                    synchronized (session) {
                        attemptedSends.increment();
                        session.sendMessage(
                                new TextMessage(statusJson)
                        );
                    }
                    successfulSends.increment();

                } catch (IOException | RuntimeException e) {
                    failedSends.increment();
                    lastSendError.set("sessionId=" + session.getId() + ": " + e);
                }
            } else {
                closedSessionsSkipped.increment();
                sessions.remove(session);
            }
        }

        reportTransmissionSummaryIfDue();
    }

    private void reportTransmissionSummaryIfDue() {

        long now = System.nanoTime();
        long lastReport = lastReportNanos.get();

        if ((lastReport == 0 || now - lastReport >= REPORT_INTERVAL_NANOS)
                && lastReportNanos.compareAndSet(lastReport, now)) {
            LOGGER.info(
                    "[WEBSOCKET] Resumen de transmisión: activeSessions={}, broadcasts={}, " +
                            "attemptedSends={}, successfulSends={}, failedSends={}, " +
                            "closedSessionsSkipped={}, lastSendError={}",
                    sessions.size(),
                    broadcasts.sum(),
                    attemptedSends.sum(),
                    successfulSends.sum(),
                    failedSends.sum(),
                    closedSessionsSkipped.sum(),
                    lastSendError.get()
            );
        }
    }
}
