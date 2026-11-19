package com.example.iot.actiondispatcher;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "operator_decisions";

    public static final String DELAYED_EXCHANGE = "operator_decisions_delayed";
    public static final String ACTUATOR_QUEUE = "critical_actions_queue";

    public static final String MAINTENANCE_QUEUE = "maintenance_queue";

    @Bean
    public RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer, ConnectionFactory connection) {
        RabbitTemplate template = new RabbitTemplate();
        configurer.configure(template, connection);
        // A delayed exchange cannot return an unroutable message at publication time.
        template.setMandatoryExpression(new SpelExpressionParser()
                .parseExpression("!messageProperties.headers.containsKey('x-delay')"));
        return template;
    }

    @Bean
    public DirectExchange operatorDecisionsExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public org.springframework.amqp.core.CustomExchange delayedDecisionsExchange() {
        return new org.springframework.amqp.core.CustomExchange(DELAYED_EXCHANGE,
                "x-delayed-message", true, false, java.util.Map.of("x-delayed-type", "direct"));
    }

    @Bean
    public Binding delayedMaintenanceBinding(Queue maintenanceQueue,
            org.springframework.amqp.core.CustomExchange delayedDecisionsExchange) {
        return BindingBuilder.bind(maintenanceQueue).to(delayedDecisionsExchange)
                .with("maintenance").noargs();
    }

    @Bean
    public Queue actuatorQueue() {
        return new Queue(ACTUATOR_QUEUE, true);
    }

    @Bean
    public Queue maintenanceQueue() {
        return new Queue(MAINTENANCE_QUEUE, true);
    }

    @Bean
    public Binding actuatorBinding(
            Queue actuatorQueue,
            DirectExchange operatorDecisionsExchange) {

        return BindingBuilder.bind(actuatorQueue)
                .to(operatorDecisionsExchange)
                .with("actuator");
    }

    @Bean
    public Binding maintenanceBinding(
            Queue maintenanceQueue,
            DirectExchange operatorDecisionsExchange) {

        return BindingBuilder.bind(maintenanceQueue)
                .to(operatorDecisionsExchange)
                .with("maintenance");
    }
}
