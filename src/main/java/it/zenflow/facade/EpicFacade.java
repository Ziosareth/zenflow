package it.zenflow.facade;

import it.zenflow.dto.EpicDTO;
import it.zenflow.mapper.EpicMapper;
import it.zenflow.model.project.Epic;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.enums.EpicStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.EpicService;
import it.zenflow.service.ProjectService;
import it.zenflow.service.rbac.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EpicFacade {

    private final EpicService epicService;
    private final ProjectService projectService;
    private final UserService userService;
    private final MessageSource messageSource;
    private final EpicMapper epicMapper;

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Project> getProjectById(Long id) { return projectService.findById(id); }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<User> getUserByUsername(String username) { return userService.findByUsername(username); }

    public boolean isProjectOwner(Project project, User user) {
        return project.getOwner() != null && project.getOwner().getId() != null && project.getOwner().getId().equals(user.getId());
    }

    public boolean isTeamMember(Project project, User user) {
        return project.getTeamMembers() != null && project.getTeamMembers().contains(user);
    }

    public boolean hasAdminRights(Project project, User user, UserDetails userDetails) {
        return isProjectOwner(project, user) || userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Epic> findByProject(Project project) { return epicService.findByProject(project); }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Epic createEpic(EpicDTO dto, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(dto.getProjectId()).orElseThrow(() -> new RuntimeException("Project not found"));

        if (!isProjectOwner(project, currentUser) && !isTeamMember(project, currentUser) &&
                userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to create epics for this project");
        }

        Epic epic = epicMapper.toEntity(dto);
        epic.setProject(project);
        if (epic.getStatus() == null) {
            epic.setStatus(EpicStatus.PLANNED);
        }
        return epicService.save(epic);
    }

    public EpicDTO mapToDTO(Epic epic) { return epicMapper.toDto(epic); }

    public String getLocalizedMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}
