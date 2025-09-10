package it.zenflow.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
class SnapshotScheduler {

    private final SnapshotService snapshotService;


    // Esegui una volta all'avvio dell'applicazione
    @EventListener(ApplicationReadyEvent.class)
    void runOnStartup() {
        snapshotService.snapshotActiveSprintsAt(LocalDate.now());
    }

    // Run daily at 18:00 Europe/Rome
    @Scheduled(cron = "0 0 18 * * *", zone = "Europe/Rome")
    void runDaily() {
        snapshotService.snapshotActiveSprintsAt(LocalDate.now());
    }
}
