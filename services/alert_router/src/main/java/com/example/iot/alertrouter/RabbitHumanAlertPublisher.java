package com.example.iot.alertrouter;

import com.example.iot.alertrouter.dto.RoutedAlert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
class RabbitHumanAlertPublisher implements HumanAlertPublisher {
    private final RabbitTemplate rabbit;
    private final ObjectMapper mapper;

    RabbitHumanAlertPublisher(RabbitTemplate rabbit, ObjectMapper mapper) {
        this.rabbit = rabbit;
        this.mapper = mapper;
    }

    @Override
    public void publish(RoutedAlert alert) throws JsonProcessingException {
        rabbit.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "", mapper.writeValueAsString(alert));
    }
}
