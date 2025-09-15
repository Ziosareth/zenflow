package it.zenflow.service;

import it.zenflow.model.project.Milestone;
import it.zenflow.model.project.MilestoneRepository;
import it.zenflow.model.project.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Milestone> findById(Long id) {
        return milestoneRepository.findById(id);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Milestone> findByProject(Project project) {
        return milestoneRepository.findByProject(project);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Milestone save(Milestone milestone) {
        return milestoneRepository.save(milestone);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteById(Long id) {
        milestoneRepository.deleteById(id);
    }
}
