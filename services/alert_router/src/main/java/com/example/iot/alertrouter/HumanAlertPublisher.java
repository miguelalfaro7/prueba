package com.example.iot.alertrouter;

import com.example.iot.alertrouter.dto.RoutedAlert;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface HumanAlertPublisher {
    void publish(RoutedAlert alert) throws JsonProcessingException;
}
