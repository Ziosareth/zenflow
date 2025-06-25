package it.zenflow.model.master;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, String> {
    List<Tenant> findByEnabledTrue();

}
