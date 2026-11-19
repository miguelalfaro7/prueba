package com.example.iot.actiondispatcher;

import com.example.iot.actiondispatcher.dto.OperatorDecisionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Table(name = "operator_decisions")
public class DecisionAuditEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "alert_id")
    private String alertId;
    @Column(name = "sensor_id")
    private String sensorId;
    private String action;
    @Column(name = "routing_key")
    private String routingKey;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

    protected DecisionAuditEntry() {}

    DecisionAuditEntry(OperatorDecisionRequest decision, JsonNode payload) {
        alertId = decision.alertId();
        sensorId = decision.sensorId();
        action = decision.action().name();
        routingKey = decision.action().routingKey();
        this.payload = payload;
    }
}
