package it.zenflow.config.multitenant;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Optional;

@Profile("!test")
@Configuration
@RequiredArgsConstructor
public class MultitenantConfiguration {

    private final TenantDataSourcePool pool;      // iniettato
    @Value("${defaultTenant}")
    private String defaultTenant;                 // fallback

    /**
     * DataSource usato da Hibernate/Spring Data per *tutti* gli accessi
     * applicativi; instrada verso il pool corretto in base al TenantContext.
     */
    @Bean
    public DataSource routingDataSource() {

        return new AbstractRoutingDataSource() {

            /** Chiave di routing → nome tenant */
            @Override
            protected Object determineCurrentLookupKey() {
                return Optional.ofNullable(TenantContext.getCurrentTenant())
                        .orElse(defaultTenant);
            }

            /**
             * Restituisce (o crea in cache) il DataSource del tenant.
             * Non serve pre-popolare la mappa con setTargetDataSources().
             */
            @Override
            protected DataSource determineTargetDataSource() {
                String tenantName = (String) determineCurrentLookupKey();
                return pool.getOrCreate(tenantName);       // lookup dinamico
            }

            /** disabilitiamo il lenientFallback “nativo” */
            @Override
            public void afterPropertiesSet() { /* no-op */ }
        };
    }
}
