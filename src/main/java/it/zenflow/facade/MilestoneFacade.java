package it.zenflow.facade;

import it.zenflow.dto.MilestoneDTO;
import it.zenflow.mapper.MilestoneMapper;
import it.zenflow.model.project.Milestone;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.enums.MilestoneStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.MilestoneService;
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
public class MilestoneFacade {

    private final MilestoneService milestoneService;
    private final ProjectService projectService;
    private final UserService userService;
    private final MessageSource messageSource;
    private final MilestoneMapper milestoneMapper;

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
    public List<Milestone> findByProject(Project project) { return milestoneService.findByProject(project); }

    @Transactional(transactionManager = "tenantTransactionManager")
    public Milestone createMilestone(MilestoneDTO dto, User currentUser, UserDetails userDetails) {
        Project project = projectService.findById(dto.getProjectId()).orElseThrow(() -> new RuntimeException("Project not found"));

        if (!isProjectOwner(project, currentUser) && !isTeamMember(project, currentUser) &&
                userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
            throw new AccessDeniedException("Not authorized to create milestones for this project");
        }

        Milestone milestone = milestoneMapper.toEntity(dto);
        milestone.setProject(project);
        if (milestone.getStatus() == null) {
            milestone.setStatus(MilestoneStatus.PLANNED);
        }
        return milestoneService.save(milestone);
    }

    public MilestoneDTO mapToDTO(Milestone milestone) { return milestoneMapper.toDto(milestone); }

    public String getLocalizedMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}
