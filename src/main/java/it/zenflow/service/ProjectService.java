package it.zenflow.service;

import it.zenflow.model.project.Project;
import it.zenflow.model.project.ProjectRepository;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.rbac.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Project> findAll(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Project> findByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Project> findByOwner(User owner) {
        return projectRepository.findByOwner(owner);
    }

    @Transactional(readOnly = true)
    public List<Project> findUserProjects(User user, ProjectStatus status) {
        return projectRepository.findUserProjects(user, status);
    }

    @Transactional(readOnly = true)
    public List<Project> findProjectsByTeamMember(User user) {
        return projectRepository.findProjectsByTeamMember(user);
    }

    @Transactional
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    @Transactional
    public void deleteById(Long id) {
        projectRepository.deleteById(id);
    }
}