package com.example.iot.historyservice.domain;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps the original per-sensor sampling window in process memory. */
public final class SensorSamplingPolicy {

    private final long sampleSeconds;
    private final Map<String, Instant> lastAccepted = new ConcurrentHashMap<>();

    public SensorSamplingPolicy(long sampleSeconds) {
        this.sampleSeconds = sampleSeconds;
    }

    /**
     * Returns whether to store this reading and records the timestamp immediately,
     * matching the existing consumer's behavior before repository persistence.
     */
    public boolean shouldStore(String sensorId, Instant recordedAt) {
        Instant last = lastAccepted.get(sensorId);
        if (last != null && recordedAt.isBefore(last.plusSeconds(sampleSeconds))) {
            return false;
        }
        lastAccepted.put(sensorId, recordedAt);
        return true;
    }
}
