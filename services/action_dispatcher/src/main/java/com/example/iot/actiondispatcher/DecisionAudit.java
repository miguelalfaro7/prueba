package com.example.iot.actiondispatcher;

import com.example.iot.actiondispatcher.dto.OperatorDecisionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DecisionAudit {
    private final EntityManager database;
    private final DecisionAuditRepository audit;
    private final ObjectMapper mapper;

    public DecisionAudit(EntityManager database, DecisionAuditRepository audit, ObjectMapper mapper) {
        this.database = database;
        this.audit = audit;
        this.mapper = mapper;
    }

    @Transactional
    public void accept(OperatorDecisionRequest decision) {
        var payload = mapper.valueToTree(decision);
        int inserted = database.createNativeQuery("""
                INSERT INTO decision_outbox (alert_id, payload)
                VALUES (:id, CAST(:payload AS jsonb))
                ON CONFLICT (alert_id) DO NOTHING
                """)
                .setParameter("id", decision.alertId())
                .setParameter("payload", payload.toString())
                .executeUpdate();

        if (inserted == 1) {
            audit.save(new DecisionAuditEntry(decision, payload));
            return;
        }

        String existing = (String) database.createNativeQuery("""
                SELECT CAST(payload AS text) FROM decision_outbox WHERE alert_id = :id
                """)
                .setParameter("id", decision.alertId())
                .getSingleResult();
        try {
            if (!payload.equals(mapper.readTree(existing))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La alerta ya tiene otra decisión");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer el despacho registrado", e);
        }
    }
}
