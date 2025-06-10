package it.zenflow.service.rbac;

import it.zenflow.model.rbac.Permission;
import it.zenflow.model.rbac.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final MessageSource messageSource;

    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, List<Permission>> findAllGroupedByCategory() {
        return permissionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                    permission -> permission.getCategory() != null ? permission.getCategory() : "UNCATEGORIZED"
                ));
    }

    @Transactional(readOnly = true)
    public Optional<Permission> findById(Long id) {
        return permissionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Permission> findByName(String name) {
        return permissionRepository.findByName(name);
    }

    @Transactional
    public Permission save(Permission permission) {
        return permissionRepository.save(permission);
    }

    @Transactional
    public void deleteById(Long id) {
        permissionRepository.deleteById(id);
    }

    public String getLocalizedDescription(Permission permission) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage("permission." + permission.getName(), null, permission.getDescription(), locale);
    }

    public String getLocalizedCategoryName(String category) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage("permission.category." + category, null, category, locale);
    }
}
