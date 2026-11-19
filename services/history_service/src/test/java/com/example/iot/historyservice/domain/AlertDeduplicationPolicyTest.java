package com.example.iot.historyservice.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlertDeduplicationPolicyTest {

    private final AlertDeduplicationPolicy policy = new AlertDeduplicationPolicy();

    @Test
    void onlyTreatsAnExistingNonNullAlertIdAsDuplicate() {
        assertTrue(policy.isDuplicate("alert-1", true));
        assertFalse(policy.isDuplicate("alert-1", false));
        assertFalse(policy.isDuplicate(null, true));
    }
}
