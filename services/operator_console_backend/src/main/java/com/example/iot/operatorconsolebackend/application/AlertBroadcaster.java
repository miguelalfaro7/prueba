package com.example.iot.operatorconsolebackend.application;

import com.example.iot.operatorconsolebackend.dto.RoutedAlert;

@FunctionalInterface
public interface AlertBroadcaster {
    void broadcastAlert(RoutedAlert alert);
}
