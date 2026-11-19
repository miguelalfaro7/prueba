package com.example.iot.operatorconsolebackend;

import com.example.iot.operatorconsolebackend.dto.RoutedAlert;
import com.example.iot.operatorconsolebackend.application.OperatorAlertApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQAlertListener {

    private final OperatorAlertApplicationService alertService;
    private final ObjectMapper objectMapper;

    public RabbitMQAlertListener(
            OperatorAlertApplicationService alertService,
            ObjectMapper objectMapper) {

        this.alertService = alertService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queuesToDeclare = @Queue(value = "operator_alerts_queue", durable = "true"))
    public void receiveAlert(String message) throws Exception {

        RoutedAlert alert = objectMapper.readValue(message, RoutedAlert.class);

        System.out.println(
                "[RABBITMQ] Alerta recibida: " + alert.alertId()
        );

        alertService.receive(alert);
    }
}
