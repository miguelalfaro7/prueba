package com.example.iot.actiondispatcher;

import com.example.iot.actiondispatcher.dto.OperatorDecisionRequest;

public interface DecisionTransport {
    void send(OperatorDecisionRequest decision, long recordedAtMillis);
}
