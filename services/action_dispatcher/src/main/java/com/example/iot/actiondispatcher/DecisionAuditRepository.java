package com.example.iot.actiondispatcher;

import org.springframework.data.repository.Repository;

public interface DecisionAuditRepository extends Repository<DecisionAuditEntry, Long> {
    DecisionAuditEntry save(DecisionAuditEntry entry);
}
