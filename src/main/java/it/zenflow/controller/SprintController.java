package it.zenflow.controller;

import it.zenflow.dto.SprintDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.project.enums.SprintStatus;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.ProjectService;
import it.zenflow.service.SprintService;
import it.zenflow.service.UserStoryService;
import it.zenflow.service.rbac.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/projects/{projectId}/sprints")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;
    private final ProjectService projectService;
    private final UserStoryService userStoryService;
    private final UserService userService;
    private final MessageSource messageSource;

    @GetMapping("")
    @PreAuthorize("hasAuthority('READ_SPRINT')")
    public String listSprints(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> {
                    // Verifica che il progetto sia di tipo SCRUM
                    if (project.getType() == null || project.getType() != ProjectType.SCRUM) {
                        String message = messageSource.getMessage("sprint.only_for_scrum", null, LocaleContextHolder.getLocale());
                        model.addAttribute("errorMessage", message);

                        // Safely set isOwner and isTeamMember attributes
                        boolean isOwner = project.getOwner() != null && 
                                         project.getOwner().getId() != null && 
                                         project.getOwner().getId().equals(currentUser.getId());
                        model.addAttribute("isOwner", isOwner);

                        boolean isTeamMember = project.getTeamMembers() != null && 
                                              project.getTeamMembers().contains(currentUser);
                        model.addAttribute("isTeamMember", isTeamMember);

                        return "projects/detail";
                    }

                    List<Sprint> sprints = sprintService.findByProject(project);

                    // Check for sprints of each status type
                    boolean hasActive = sprints.stream().anyMatch(s -> s.getStatus() == SprintStatus.ACTIVE);
                    boolean hasPlanned = sprints.stream().anyMatch(s -> s.getStatus() == SprintStatus.PLANNED);
                    boolean hasCompleted = sprints.stream().anyMatch(s -> s.getStatus() == SprintStatus.COMPLETED);

                    model.addAttribute("project", project);
                    model.addAttribute("sprints", sprints);
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("isOwner", project.getOwner().getId().equals(currentUser.getId()));
                    model.addAttribute("isTeamMember", project.getTeamMembers().contains(currentUser));
                    model.addAttribute("hasActive", hasActive);
                    model.addAttribute("hasPlanned", hasPlanned);
                    model.addAttribute("hasCompleted", hasCompleted);

                    return "projects/sprints/list";
                })
                .orElse("redirect:/projects");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_SPRINT')")
    public String viewSprint(@PathVariable Long projectId, @PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> sprintService.findByIdWithStories(id)
                        .map(sprint -> {
                            if (!sprint.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/sprints";
                            }

                            // Ottieni le user story non assegnate a nessuno sprint
                            List<UserStory> availableUserStories = userStoryService.findUnassignedUserStories(project);

                            model.addAttribute("project", project);
                            model.addAttribute("sprint", sprint);
                            model.addAttribute("availableUserStories", availableUserStories);
                            model.addAttribute("currentUser", currentUser);
                            model.addAttribute("isOwner", project.getOwner().getId().equals(currentUser.getId()));
                            model.addAttribute("isTeamMember", project.getTeamMembers().contains(currentUser));

                            return "projects/sprints/detail";
                        })
                        .orElse("redirect:/projects/" + projectId + "/sprints"))
                .orElse("redirect:/projects");
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_SPRINT')")
    public String newSprintForm(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> {
                    // Verifica che il progetto sia di tipo SCRUM
                    if (project.getType() == null || project.getType() != ProjectType.SCRUM) {
                        String message = messageSource.getMessage("sprint.only_for_scrum", null, LocaleContextHolder.getLocale());
                        model.addAttribute("errorMessage", message);

                        // Safely set isOwner and isTeamMember attributes
                        boolean isOwner = project.getOwner() != null && 
                                         project.getOwner().getId() != null && 
                                         project.getOwner().getId().equals(currentUser.getId());
                        model.addAttribute("isOwner", isOwner);

                        boolean isTeamMember = project.getTeamMembers() != null && 
                                              project.getTeamMembers().contains(currentUser);
                        model.addAttribute("isTeamMember", isTeamMember);

                        return "projects/detail";
                    }

                    // Verifica che l'utente sia proprietario o membro del team
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !project.getTeamMembers().contains(currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    SprintDTO sprintDTO = new SprintDTO();
                    sprintDTO.setProjectId(projectId);

                    model.addAttribute("project", project);
                    model.addAttribute("sprintDTO", sprintDTO);
                    model.addAttribute("statuses", SprintStatus.values());
                    model.addAttribute("isNew", true);

                    return "projects/sprints/form";
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_SPRINT')")
    public String createSprint(
            @PathVariable Long projectId,
            @Valid @ModelAttribute SprintDTO sprintDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> {
                    // Verifica che il progetto sia di tipo SCRUM
                    if (project.getType() == null || project.getType() != ProjectType.SCRUM) {
                        String message = messageSource.getMessage("sprint.only_for_scrum", null, LocaleContextHolder.getLocale());
                        model.addAttribute("errorMessage", message);

                        // Safely set isOwner and isTeamMember attributes
                        boolean isOwner = project.getOwner() != null && 
                                         project.getOwner().getId() != null && 
                                         project.getOwner().getId().equals(currentUser.getId());
                        model.addAttribute("isOwner", isOwner);

                        boolean isTeamMember = project.getTeamMembers() != null && 
                                              project.getTeamMembers().contains(currentUser);
                        model.addAttribute("isTeamMember", isTeamMember);

                        return "projects/detail";
                    }

                    // Verifica che l'utente sia proprietario o membro del team
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !project.getTeamMembers().contains(currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    if (bindingResult.hasErrors()) {
                        model.addAttribute("project", project);
                        model.addAttribute("statuses", SprintStatus.values());
                        model.addAttribute("isNew", true);
                        return "projects/sprints/form";
                    }

                    Sprint sprint = new Sprint();
                    sprint.setName(sprintDTO.getName());
                    sprint.setGoal(sprintDTO.getGoal());
                    sprint.setStartDate(sprintDTO.getStartDate());
                    sprint.setEndDate(sprintDTO.getEndDate());
                    sprint.setStatus(SprintStatus.PLANNED);
                    sprint.setProject(project);

                    sprintService.save(sprint);

                    String message = messageSource.getMessage("sprint.created", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);

                    return "redirect:/projects/" + projectId + "/sprints";
                })
                .orElse("redirect:/projects");
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT')")
    public String editSprintForm(@PathVariable Long projectId, @PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> sprintService.findById(id)
                        .map(sprint -> {
                            if (!sprint.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/sprints";
                            }

                            // Verifica che l'utente sia proprietario o membro del team
                            if (!project.getOwner().getId().equals(currentUser.getId()) && 
                                !project.getTeamMembers().contains(currentUser) &&
                                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                                return "redirect:/projects/" + projectId;
                            }

                            SprintDTO sprintDTO = new SprintDTO();
                            sprintDTO.setId(sprint.getId());
                            sprintDTO.setName(sprint.getName());
                            sprintDTO.setGoal(sprint.getGoal());
                            sprintDTO.setStartDate(sprint.getStartDate());
                            sprintDTO.setEndDate(sprint.getEndDate());
                            sprintDTO.setStatus(sprint.getStatus());
                            sprintDTO.setProjectId(projectId);
                            sprintDTO.setPlannedStoryPoints(sprint.getPlannedStoryPoints());
                            sprintDTO.setCompletedStoryPoints(sprint.getCompletedStoryPoints());
                            sprintDTO.setSprintVelocity(sprint.getSprintVelocity());

                            model.addAttribute("project", project);
                            model.addAttribute("sprintDTO", sprintDTO);
                            model.addAttribute("statuses", SprintStatus.values());
                            model.addAttribute("isNew", false);

                            return "projects/sprints/form";
                        })
                        .orElse("redirect:/projects/" + projectId + "/sprints"))
                .orElse("redirect:/projects");
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT')")
    public String updateSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @ModelAttribute SprintDTO sprintDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> sprintService.findById(id)
                        .map(sprint -> {
                            if (!sprint.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/sprints";
                            }

                            // Verifica che l'utente sia proprietario o membro del team
                            if (!project.getOwner().getId().equals(currentUser.getId()) && 
                                !project.getTeamMembers().contains(currentUser) &&
                                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                                return "redirect:/projects/" + projectId;
                            }

                            if (bindingResult.hasErrors()) {
                                model.addAttribute("project", project);
                                model.addAttribute("statuses", SprintStatus.values());
                                model.addAttribute("isNew", false);
                                return "projects/sprints/form";
                            }

                            sprint.setName(sprintDTO.getName());
                            sprint.setGoal(sprintDTO.getGoal());
                            sprint.setStartDate(sprintDTO.getStartDate());
                            sprint.setEndDate(sprintDTO.getEndDate());
                            // Non aggiorniamo lo status qui, ci sono metodi specifici per farlo

                            sprintService.save(sprint);

                            String message = messageSource.getMessage("sprint.updated", null, LocaleContextHolder.getLocale());
                            redirectAttributes.addFlashAttribute("message", message);

                            return "redirect:/projects/" + projectId + "/sprints/" + id;
                        })
                        .orElse("redirect:/projects/" + projectId + "/sprints"))
                .orElse("redirect:/projects");
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT')")
    public String startSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> {
                    // Verifica che l'utente sia proprietario o membro del team
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !project.getTeamMembers().contains(currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    sprintService.startSprint(id);

                    String message = messageSource.getMessage("sprint.started", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);

                    return "redirect:/projects/" + projectId + "/sprints/" + id;
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT')")
    public String completeSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> {
                    // Verifica che l'utente sia proprietario o membro del team
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !project.getTeamMembers().contains(currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    sprintService.completeSprint(id);

                    String message = messageSource.getMessage("sprint.completed_message", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);

                    return "redirect:/projects/" + projectId + "/sprints/" + id;
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT')")
    public String cancelSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> {
                    // Verifica che l'utente sia proprietario o membro del team
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !project.getTeamMembers().contains(currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    sprintService.cancelSprint(id);

                    String message = messageSource.getMessage("sprint.cancelled", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);

                    return "redirect:/projects/" + projectId + "/sprints/" + id;
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/{sprintId}/add-story/{storyId}")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT') and hasAuthority('UPDATE_USER_STORY')")
    public String addUserStoryToSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long storyId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> {
                    // Verifica che l'utente sia proprietario o membro del team
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !project.getTeamMembers().contains(currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    sprintService.addUserStoryToSprint(sprintId, storyId);

                    String message = messageSource.getMessage("sprint.story_added", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);

                    return "redirect:/projects/" + projectId + "/sprints/" + sprintId;
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/{sprintId}/remove-story/{storyId}")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT') and hasAuthority('UPDATE_USER_STORY')")
    public String removeUserStoryFromSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long storyId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> {
                    // Verifica che l'utente sia proprietario o membro del team
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !project.getTeamMembers().contains(currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    sprintService.removeUserStoryFromSprint(sprintId, storyId);

                    String message = messageSource.getMessage("sprint.story_removed", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);

                    return "redirect:/projects/" + projectId + "/sprints/" + sprintId;
                })
                .orElse("redirect:/projects");
    }
}
