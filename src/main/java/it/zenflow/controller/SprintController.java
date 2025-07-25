package it.zenflow.controller;

import it.zenflow.dto.SprintDTO;
import it.zenflow.facade.SprintFacade;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.SprintStatus;
import it.zenflow.model.rbac.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

    private final SprintFacade sprintFacade;

    @GetMapping("")
    @PreAuthorize("hasAuthority('READ_SPRINT')")
    public String listSprints(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return sprintFacade.getProjectById(projectId)
                .map(project -> {
                    // Verify that the project is of type SCRUM
                    if (!sprintFacade.isProjectScrum(project)) {
                        String message = sprintFacade.getLocalizedMessage("sprint.only_for_scrum");
                        model.addAttribute("errorMessage", message);

                        model.addAttribute("isOwner", sprintFacade.isProjectOwner(project, currentUser));
                        model.addAttribute("isTeamMember", sprintFacade.isTeamMember(project, currentUser));

                        return "projects/detail";
                    }

                    List<Sprint> sprints = sprintFacade.findByProject(project);

                    // Check for sprints of each status type
                    boolean hasActive = sprintFacade.hasSprintsWithStatus(sprints, SprintStatus.ACTIVE);
                    boolean hasPlanned = sprintFacade.hasSprintsWithStatus(sprints, SprintStatus.PLANNED);
                    boolean hasCompleted = sprintFacade.hasSprintsWithStatus(sprints, SprintStatus.COMPLETED);

                    model.addAttribute("project", project);
                    model.addAttribute("sprints", sprints);
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("isOwner", sprintFacade.isProjectOwner(project, currentUser));
                    model.addAttribute("isTeamMember", sprintFacade.isTeamMember(project, currentUser));
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
        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return sprintFacade.getProjectById(projectId)
                .map(project -> sprintFacade.findByIdWithStories(id)
                        .map(sprint -> {
                            if (!sprint.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/sprints";
                            }

                            // Get user stories not assigned to any sprint
                            List<UserStory> availableUserStories = sprintFacade.findUnassignedUserStories(project);

                            model.addAttribute("project", project);
                            model.addAttribute("sprint", sprint);
                            model.addAttribute("availableUserStories", availableUserStories);
                            model.addAttribute("currentUser", currentUser);
                            model.addAttribute("isOwner", sprintFacade.isProjectOwner(project, currentUser));
                            model.addAttribute("isTeamMember", sprintFacade.isTeamMember(project, currentUser));

                            return "projects/sprints/detail";
                        })
                        .orElse("redirect:/projects/" + projectId + "/sprints"))
                .orElse("redirect:/projects");
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_SPRINT')")
    public String newSprintForm(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return sprintFacade.getProjectById(projectId)
                .map(project -> {
                    // Verify that the project is of type SCRUM
                    if (!sprintFacade.isProjectScrum(project)) {
                        String message = sprintFacade.getLocalizedMessage("sprint.only_for_scrum");
                        model.addAttribute("errorMessage", message);

                        model.addAttribute("isOwner", sprintFacade.isProjectOwner(project, currentUser));
                        model.addAttribute("isTeamMember", sprintFacade.isTeamMember(project, currentUser));

                        return "projects/detail";
                    }

                    // Check if user is owner or team member
                    if (!sprintFacade.isProjectOwner(project, currentUser) && 
                        !sprintFacade.isTeamMember(project, currentUser) &&
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

        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            return sprintFacade.getProjectById(projectId)
                    .map(project -> {
                        model.addAttribute("project", project);
                        model.addAttribute("statuses", SprintStatus.values());
                        model.addAttribute("isNew", true);
                        return "projects/sprints/form";
                    })
                    .orElse("redirect:/projects");
        }

        try {
            sprintFacade.createSprint(sprintDTO, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", sprintFacade.getLocalizedMessage("sprint.created"));
            return "redirect:/projects/" + projectId + "/sprints";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/sprints";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT')")
    public String editSprintForm(@PathVariable Long projectId, @PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return sprintFacade.getProjectById(projectId)
                .map(project -> sprintFacade.findById(id)
                        .map(sprint -> {
                            if (!sprint.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/sprints";
                            }

                            // Check if user is owner or team member
                            if (!sprintFacade.isProjectOwner(project, currentUser) && 
                                !sprintFacade.isTeamMember(project, currentUser) &&
                                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                                return "redirect:/projects/" + projectId;
                            }

                            SprintDTO sprintDTO = sprintFacade.mapToDTO(sprint);

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

        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            return sprintFacade.getProjectById(projectId)
                    .map(project -> {
                        model.addAttribute("project", project);
                        model.addAttribute("statuses", SprintStatus.values());
                        model.addAttribute("isNew", false);
                        return "projects/sprints/form";
                    })
                    .orElse("redirect:/projects");
        }

        try {
            sprintFacade.updateSprint(id, sprintDTO, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", sprintFacade.getLocalizedMessage("sprint.updated"));
            return "redirect:/projects/" + projectId + "/sprints/" + id;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/sprints";
        }
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT')")
    public String startSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            sprintFacade.startSprint(projectId, id, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", sprintFacade.getLocalizedMessage("sprint.started"));
            return "redirect:/projects/" + projectId + "/sprints/" + id;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/sprints";
        }
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT')")
    public String completeSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            sprintFacade.completeSprint(projectId, id, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", sprintFacade.getLocalizedMessage("sprint.completed_message"));
            return "redirect:/projects/" + projectId + "/sprints/" + id;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/sprints";
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT')")
    public String cancelSprint(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            sprintFacade.cancelSprint(projectId, id, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", sprintFacade.getLocalizedMessage("sprint.cancelled"));
            return "redirect:/projects/" + projectId + "/sprints/" + id;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/sprints";
        }
    }

    @PostMapping("/{sprintId}/add-story/{storyId}")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT') and hasAuthority('UPDATE_USER_STORY')")
    public String addUserStoryToSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long storyId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            sprintFacade.addUserStoryToSprint(projectId, sprintId, storyId, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", sprintFacade.getLocalizedMessage("sprint.story_added"));
            return "redirect:/projects/" + projectId + "/sprints/" + sprintId;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/sprints/" + sprintId;
        }
    }

    @PostMapping("/{sprintId}/remove-story/{storyId}")
    @PreAuthorize("hasAuthority('UPDATE_SPRINT') and hasAuthority('UPDATE_USER_STORY')")
    public String removeUserStoryFromSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @PathVariable Long storyId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = sprintFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            sprintFacade.removeUserStoryFromSprint(projectId, sprintId, storyId, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", sprintFacade.getLocalizedMessage("sprint.story_removed"));
            return "redirect:/projects/" + projectId + "/sprints/" + sprintId;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/sprints/" + sprintId;
        }
    }
}