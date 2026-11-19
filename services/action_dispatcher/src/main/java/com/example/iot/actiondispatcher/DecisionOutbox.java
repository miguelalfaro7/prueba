package com.example.iot.actiondispatcher;

import com.example.iot.actiondispatcher.dto.OperatorDecisionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionOutbox {
    private final EntityManager database;
    private final ObjectMapper mapper;
    private final DecisionTransport transport;

    public DecisionOutbox(EntityManager database, ObjectMapper mapper, DecisionTransport transport) {
        this.database = database;
        this.mapper = mapper;
        this.transport = transport;
    }

    @Scheduled(fixedDelayString = "${iot.dispatch.poll-ms:500}")
    @Transactional
    public void dispatchNext() {
        var rows = database.createNativeQuery("""
                SELECT alert_id, CAST(payload AS text),
                    CAST(EXTRACT(EPOCH FROM created_at) * 1000 AS bigint)
                FROM decision_outbox
                WHERE sent_at IS NULL AND next_attempt_at <= now()
                ORDER BY next_attempt_at, created_at
                LIMIT 1 FOR UPDATE SKIP LOCKED
                """).getResultList();
        if (rows.isEmpty()) {
            return;
        }
        Object[] row = (Object[]) rows.get(0);
        try {
            transport.send(mapper.readValue((String) row[1], OperatorDecisionRequest.class),
                    ((Number) row[2]).longValue());
        } catch (JsonProcessingException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(DecisionOutbox.class)
                    .warn("No se pudo despachar la alerta {}; se reintentará", row[0], e);
            database.createNativeQuery("""
                    UPDATE decision_outbox SET next_attempt_at = now() + interval '5 seconds'
                    WHERE alert_id = :id
                    """).setParameter("id", row[0]).executeUpdate();
            return;
        }
        database.createNativeQuery("UPDATE decision_outbox SET sent_at = now() WHERE alert_id = :id")
                .setParameter("id", row[0]).executeUpdate();
    }
}
