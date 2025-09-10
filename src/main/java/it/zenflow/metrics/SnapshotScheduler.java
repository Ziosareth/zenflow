package it.zenflow.metrics;

import it.zenflow.config.multitenant.TenantContext;
import it.zenflow.master.model.Tenant;
import it.zenflow.master.model.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
class SnapshotScheduler {

    private final SnapshotService snapshotService;
    private final TenantRepository tenantRepository;

    // Esegui una volta all'avvio dell'applicazione, per tutti i tenant (escluso master)
    @EventListener(ApplicationReadyEvent.class)
    void runOnStartup() {
        runForAllTenants();
    }

    // Run daily at 18:00 Europe/Rome, per tutti i tenant (escluso master)
    @Scheduled(cron = "0 0 18 * * *", zone = "Europe/Rome")
    void runDaily() {
        runForAllTenants();
    }

    private void runForAllTenants() {
        List<Tenant> tenants = tenantRepository.findByEnabledTrue();
        LocalDate today = LocalDate.now();
        for (Tenant tenant : tenants) {
            try {
                TenantContext.setCurrentTenant(tenant.getName());
                snapshotService.snapshotActiveSprintsAt(today);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
