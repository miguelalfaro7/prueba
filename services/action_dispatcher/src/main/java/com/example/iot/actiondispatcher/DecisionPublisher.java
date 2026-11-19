package com.example.iot.actiondispatcher;

import com.example.iot.actiondispatcher.dto.DecisionAction;
import com.example.iot.actiondispatcher.dto.OperatorDecisionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DecisionPublisher implements DecisionTransport {
    private final RabbitTemplate rabbit;
    private final ObjectMapper mapper;
    private final long maintenanceDelayMillis;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DecisionPublisher(RabbitTemplate rabbit, ObjectMapper mapper,
            @Value("${iot.maintenance.delay:24h}") Duration maintenanceDelay) {
        this(rabbit, mapper, maintenanceDelay, Clock.systemUTC());
    }

    DecisionPublisher(RabbitTemplate rabbit, ObjectMapper mapper, Duration delay, Clock clock) {
        this.rabbit = rabbit;
        this.mapper = mapper;
        this.maintenanceDelayMillis = delay.toMillis();
        if (maintenanceDelayMillis < 1 || maintenanceDelayMillis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("El retraso de mantenimiento debe caber en x-delay");
        }
        this.clock = clock;
    }

    @Override
    public void send(OperatorDecisionRequest decision, long recordedAtMillis) {
        boolean scheduled = decision.action() == DecisionAction.RECONOCER_Y_ESPERAR_24H;
        long remaining = scheduled
                ? Math.max(0, maintenanceDelayMillis - Math.max(0, clock.millis() - recordedAtMillis))
                : 0;
        String exchange = remaining > 0 ? RabbitMQConfig.DELAYED_EXCHANGE : RabbitMQConfig.EXCHANGE_NAME;
        CorrelationData correlation = new CorrelationData();

        try {
            rabbit.convertAndSend(exchange, decision.action().routingKey(),
                    mapper.writeValueAsString(decision), message -> {
                        var properties = message.getMessageProperties();
                        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        properties.setMessageId(decision.alertId());
                        if (remaining > 0) {
                            properties.setHeader("x-delay", (int) remaining);
                        }
                        return message;
                    }, correlation);
            var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ rechazó el despacho o no encontró la ruta");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Confirmación de RabbitMQ interrumpida", e);
        } catch (JsonProcessingException | java.util.concurrent.ExecutionException
                | java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("No se pudo confirmar el despacho", e);
        }
    }
}
