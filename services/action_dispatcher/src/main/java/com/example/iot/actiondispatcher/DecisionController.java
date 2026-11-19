package com.example.iot.actiondispatcher;

import com.example.iot.actiondispatcher.dto.DecisionResponse;
import com.example.iot.actiondispatcher.dto.OperatorDecisionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DecisionController {
    private final DecisionAudit decisions;

    public DecisionController(DecisionAudit decisions) {
        this.decisions = decisions;
    }

    @PostMapping("/decide")
    public ResponseEntity<DecisionResponse> receiveDecision(
            @Valid @RequestBody OperatorDecisionRequest decision) {
        decisions.accept(decision);
        return ResponseEntity.accepted().body(
                new DecisionResponse("ACCEPTED", "Decision registrada para despacho"));
    }
}
