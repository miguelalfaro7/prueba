package com.example.iot.historyservice.domain;

public final class AlertDeduplicationPolicy {

    public boolean isDuplicate(String alertId, boolean alertIdAlreadyExists) {
        return alertId != null && alertIdAlreadyExists;
    }
}
