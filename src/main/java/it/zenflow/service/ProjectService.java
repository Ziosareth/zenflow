package it.zenflow.service;

import it.zenflow.model.project.Project;
import it.zenflow.model.project.ProjectRepository;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.rbac.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Page<Project> findAll(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Project> findById(Long id) {
        return projectRepository.findByIdWithOwner(id);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Project> findByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Project> findByOwner(User owner) {
        return projectRepository.findByOwnerWithOwner(owner);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Project> findUserProjects(User user, ProjectStatus status) {
        return projectRepository.findUserProjects(user, status);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Project> findProjectsByTeamMember(User user) {
        return projectRepository.findProjectsByTeamMember(user);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteById(Long id) {
        projectRepository.deleteById(id);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Project> findAllWithOwners() {
        return projectRepository.findAllWithOwners();
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Page<Project> findAllWithOwners(Pageable pageable) {
        // First get the page with normal loading
        Page<Project> projectPage = projectRepository.findAll(pageable);

        // If the page is empty, return it as is
        if (projectPage.isEmpty()) {
            return projectPage;
        }

        // Get the IDs of the projects in the page
        List<Long> projectIds = projectPage.getContent().stream()
            .map(Project::getId)
            .collect(Collectors.toList());

        // Fetch the same projects but with owners eagerly loaded
        List<Project> projectsWithOwners = projectRepository.findByIdInWithOwners(projectIds);

        // Create a new page with the same metadata but with the projects that have owners loaded
        return new PageImpl<>(projectsWithOwners, pageable, projectPage.getTotalElements());
    }
}
