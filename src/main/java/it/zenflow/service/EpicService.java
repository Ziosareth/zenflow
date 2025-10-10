package it.zenflow.service;

import it.zenflow.model.project.Epic;
import it.zenflow.model.project.EpicRepository;
import it.zenflow.model.project.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EpicService {

    private final EpicRepository epicRepository;

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Epic> findById(Long id) { return epicRepository.findById(id); }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Epic> findByProject(Project project) { return epicRepository.findByProject(project); }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Epic save(Epic epic) { return epicRepository.save(epic); }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteById(Long id) { epicRepository.deleteById(id); }
}
