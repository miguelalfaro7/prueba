package com.example.iot.actiondispatcher;

import com.example.iot.actiondispatcher.dto.DecisionAction;
import com.example.iot.actiondispatcher.dto.OperatorDecisionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionPublisherTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final ObjectMapper mapper = new ObjectMapper();
    @Mock private RabbitTemplate rabbit;
    private DecisionPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new DecisionPublisher(rabbit, mapper, Duration.ofHours(24),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void immediateDecisionPreservesPayloadAndHasNoDelay() throws Exception {
        confirm(true);
        publisher.send(new OperatorDecisionRequest("alert-1", "sensor_A",
                DecisionAction.APAGADO_INMEDIATO), NOW.toEpochMilli());

        var payload = ArgumentCaptor.forClass(String.class);
        var processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq("actuator"),
                payload.capture(), processor.capture(), any(CorrelationData.class));
        var json = mapper.readTree(payload.getValue());
        assertEquals(3, json.size());
        assertEquals("alert-1", json.get("alert_id").asText());
        assertEquals("sensor_A", json.get("sensor_id").asText());
        assertEquals("APAGADO_INMEDIATO", json.get("action").asText());
        assertFalse(processor.getValue().postProcessMessage(new Message(new byte[0]))
                .getMessageProperties().getHeaders().containsKey("x-delay"));
    }

    @Test
    void delayedDecisionSubtractsTimeAlreadySpentInTheOutbox() {
        confirm(true);
        publisher.send(new OperatorDecisionRequest("alert-1", "sensor_A",
                DecisionAction.RECONOCER_Y_ESPERAR_24H), NOW.minusSeconds(10).toEpochMilli());

        var processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(eq(RabbitMQConfig.DELAYED_EXCHANGE), eq("maintenance"),
                anyString(), processor.capture(), any(CorrelationData.class));
        var properties = processor.getValue().postProcessMessage(new Message(new byte[0]))
                .getMessageProperties();
        assertEquals(86_390_000, properties.getHeaders().get("x-delay"));
        assertEquals("alert-1", properties.getMessageId());
    }

    @Test
    void brokerRejectionLeavesTheDecisionEligibleForRetry() {
        confirm(false);
        assertThrows(IllegalStateException.class, () ->
                publisher.send(new OperatorDecisionRequest("alert-1", "sensor_A",
                        DecisionAction.PROGRAMAR_MANTENIMIENTO_AHORA), NOW.toEpochMilli()));
    }

    private void confirm(boolean ack) {
        doAnswer(call -> {
            CorrelationData data = call.getArgument(4);
            data.getFuture().complete(new CorrelationData.Confirm(ack, null));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), anyString(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }
}
