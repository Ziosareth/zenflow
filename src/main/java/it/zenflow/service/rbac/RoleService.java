package it.zenflow.service.rbac;

import it.zenflow.model.rbac.Role;
import it.zenflow.model.rbac.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Page<Role> findAll(Pageable pageable) {
        return roleRepository.findAll(pageable);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Role save(Role role) {
        return roleRepository.save(role);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteById(Long id) {
        roleRepository.deleteById(id);
    }
}
