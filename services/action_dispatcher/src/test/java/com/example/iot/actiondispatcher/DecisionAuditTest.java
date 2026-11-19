package com.example.iot.actiondispatcher;

import com.example.iot.actiondispatcher.dto.DecisionAction;
import com.example.iot.actiondispatcher.dto.OperatorDecisionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionAuditTest {
    private static final OperatorDecisionRequest DECISION =
            new OperatorDecisionRequest("alert-1", "sensor_A", DecisionAction.APAGADO_INMEDIATO);

    @Mock private EntityManager database;
    @Mock private DecisionAuditRepository repository;
    @Mock private Query insert;
    @Mock private Query lookup;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void newDecisionCreatesOneAuditEntry() {
        pendingInsert(1);
        new DecisionAudit(database, repository, mapper).accept(DECISION);
        verify(repository).save(any(DecisionAuditEntry.class));
    }

    @Test
    void identicalRetryDoesNotCreateAnotherEntry() throws Exception {
        pendingInsert(0);
        existingDecision();
        new DecisionAudit(database, repository, mapper).accept(DECISION);
        verifyNoInteractions(repository);
    }

    @Test
    void differentDecisionForTheSameAlertIsRejected() throws Exception {
        pendingInsert(0);
        existingDecision();
        var audit = new DecisionAudit(database, repository, mapper);
        var different = new OperatorDecisionRequest("alert-1", "sensor_A",
                DecisionAction.IGNORAR_10_MINUTOS);

        var error = assertThrows(ResponseStatusException.class, () -> audit.accept(different));

        assertEquals(409, error.getStatusCode().value());
        verifyNoInteractions(repository);
    }

    private void pendingInsert(int result) {
        when(database.createNativeQuery(contains("INSERT INTO decision_outbox"))).thenReturn(insert);
        when(insert.setParameter("id", "alert-1")).thenReturn(insert);
        when(insert.setParameter(org.mockito.ArgumentMatchers.eq("payload"), anyString())).thenReturn(insert);
        when(insert.executeUpdate()).thenReturn(result);
    }

    private void existingDecision() throws Exception {
        when(database.createNativeQuery(contains("SELECT CAST(payload"))).thenReturn(lookup);
        when(lookup.setParameter("id", "alert-1")).thenReturn(lookup);
        when(lookup.getSingleResult()).thenReturn(mapper.writeValueAsString(DECISION));
    }
}
