package com.example.iot.operatorconsolebackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.example.iot.operatorconsolebackend.dto.RoutedAlert;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class PendingAlertStoreCharacterizationTest {

    @Test
    void preservesInsertionOrderAndRepeatedAlertIdsInSnapshots() {
        PendingAlertStore store = new PendingAlertStore();
        RoutedAlert firstDelivery = alert("alert-1");
        RoutedAlert secondDelivery = alert("alert-1");
        RoutedAlert thirdDelivery = alert("alert-2");

        store.addAlert(firstDelivery);
        store.addAlert(secondDelivery);
        store.addAlert(thirdDelivery);
        List<RoutedAlert> snapshot = store.getPendingAlerts();

        assertEquals(List.of(firstDelivery, secondDelivery, thirdDelivery), snapshot);
        assertNotSame(snapshot, store.getPendingAlerts());
        snapshot.clear();
        assertEquals(3, store.getPendingAlerts().size());
    }

    @Test
    void concurrentRegistrationsAreNotLost() throws Exception {
        PendingAlertStore store = new PendingAlertStore();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> registrations = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            int id = index;
            registrations.add(executor.submit(() -> store.addAlert(alert("alert-" + id))));
        }
        for (Future<?> registration : registrations) {
            registration.get();
        }
        executor.shutdown();

        assertEquals(100, store.getPendingAlerts().size());
    }

    private RoutedAlert alert(String id) {
        return new RoutedAlert(id, "sensor_A", "CRITICAL", 91.0,
                "2026-01-01T00:00:00Z", List.of("APAGADO_INMEDIATO"));
    }
}
