package it.zenflow.controller;

import it.zenflow.dto.GanttItemDTO;
import it.zenflow.dto.ProjectDTO;
import it.zenflow.facade.ProjectFacade;
import it.zenflow.model.project.*;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.rbac.User;
import it.zenflow.service.EpicService;
import it.zenflow.service.MilestoneService;
import it.zenflow.service.UserStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/projects")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectFacade projectFacade;
    private final MessageSource messageSource;

    // Services used to build the Gantt data
    private final MilestoneService milestoneService;
    private final EpicService epicService;
    private final UserStoryService userStoryService;

    @GetMapping("")
    public String listProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        User currentUser = projectFacade.getCurrentUser(userDetails);
        Page<Project> projectPage = projectFacade.getAllProjects(pageable);

        model.addAttribute("projects", projectPage.getContent());
        model.addAttribute("currentPage", projectPage.getNumber());
        model.addAttribute("totalPages", projectPage.getTotalPages());
        model.addAttribute("totalItems", projectPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sort);
        model.addAttribute("currentUser", currentUser);

        return "projects/list";
    }

    @GetMapping("/{id}")
    public String viewProject(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = projectFacade.getCurrentUser(userDetails);

        return projectFacade.getProjectById(id)
                .map(project -> {
                    model.addAttribute("project", project);
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("isOwner", projectFacade.isProjectOwner(project, currentUser));
                    model.addAttribute("isTeamMember", projectFacade.isTeamMember(project, currentUser));
                    return "projects/detail";
                })
                .orElse("redirect:/projects");
    }

    @GetMapping("/{id}/gantt")
    @ResponseBody
    public ResponseEntity<List<GanttItemDTO>> getProjectGantt(@PathVariable Long id) {
        return projectFacade.getProjectById(id)
                .map(project -> {
                    List<GanttItemDTO> items = new ArrayList<>();

                    // Epics (use epic dates or fallback to project dates if both present)
                    for (Epic epic : epicService.findByProject(project)) {
                        LocalDate start = epic.getStartDate();
                        LocalDate end = epic.getDueDate();
                        if (start == null || end == null) {
                            if (project.getStartDate() != null && project.getEndDate() != null) {
                                start = project.getStartDate();
                                end = project.getEndDate();
                            } else {
                                // Skip epics without a clear time range
                                continue;
                            }
                        }
                        items.add(new GanttItemDTO(
                                "epic-" + epic.getId(),
                                epic.getTitle(),
                                start,
                                end,
                                0,
                                "EPIC",
                                null
                        ));
                    }

                    // Milestones (single-day items; use target date)
                    for (Milestone milestone : milestoneService.findByProject(project)) {
                        LocalDate date = milestone.getTargetDate();
                        int progress = milestone.isAchieved() ? 100 : 0;
                        items.add(new GanttItemDTO(
                                "milestone-" + milestone.getId(),
                                milestone.getName(),
                                date,
                                date,
                                progress,
                                "MILESTONE",
                                null
                        ));
                    }

                    // User stories (use sprint timebox if assigned)
                    for (UserStory story : userStoryService.findByProjectWithSprint(project)) {
                        Sprint sprint = story.getSprint();
                        if (sprint != null && sprint.getStartDate() != null && sprint.getEndDate() != null) {
                            items.add(new GanttItemDTO(
                                    "story-" + story.getId(),
                                    story.getTitle(),
                                    sprint.getStartDate(),
                                    sprint.getEndDate(),
                                    0,
                                    "STORY",
                                    null
                            ));
                        }
                    }

                    return ResponseEntity.ok(items);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_PROJECT')")
    public String newProjectForm(Model model) {
        prepareFormModel(model, true);
        return "projects/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_PROJECT')")
    public String createProject(
            @Valid @ModelAttribute ProjectDTO projectDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, true);
            return "projects/form";
        }

        User currentUser = projectFacade.getCurrentUser(userDetails);

        try {
            projectFacade.createProject(projectDTO, currentUser);
            addSuccessMessage(redirectAttributes, "project.created");
            return "redirect:/projects";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            prepareFormModel(model, true);
            return "projects/form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_PROJECT')")
    public String editProjectForm(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = projectFacade.getCurrentUser(userDetails);

        return projectFacade.getProjectById(id)
                .map(project -> {
                    // Check if user is owner or has admin rights
                    if (!projectFacade.hasAdminRights(project, currentUser, userDetails)) {
                        return "redirect:/projects";
                    }

                    ProjectDTO projectDTO = projectFacade.mapToDTO(project);
                    model.addAttribute("projectDTO", projectDTO);
                    prepareFormModel(model, false);
                    return "projects/form";
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_PROJECT')")
    public String updateProject(
            @PathVariable Long id,
            @Valid @ModelAttribute ProjectDTO projectDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, false);
            return "projects/form";
        }

        User currentUser = projectFacade.getCurrentUser(userDetails);

        try {
            projectFacade.updateProject(id, projectDTO, currentUser, userDetails);
            addSuccessMessage(redirectAttributes, "project.updated");
            return "redirect:/projects/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            prepareFormModel(model, false);
            return "projects/form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('DELETE_PROJECT')")
    public String deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = projectFacade.getCurrentUser(userDetails);

        try {
            projectFacade.deleteProject(id, currentUser, userDetails);
            addSuccessMessage(redirectAttributes, "project.deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/projects";
    }

    @GetMapping("/my")
    public String myProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        User currentUser = projectFacade.getCurrentUser(userDetails);
        List<Project> projects = projectFacade.getUserProjects(currentUser);

        model.addAttribute("projects", projects);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isMyProjects", true);

        // Add pagination attributes to avoid template errors
        model.addAttribute("totalPages", 1);
        model.addAttribute("totalItems", projects.size());
        model.addAttribute("currentPage", 0);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sort);

        return "projects/list";
    }
    
    /**
     * Prepares the model with common attributes for the form
     */
    private void prepareFormModel(Model model, boolean isNew) {
        if (!model.containsAttribute("projectDTO")) {
            model.addAttribute("projectDTO", new ProjectDTO());
        }
        model.addAttribute("allUsers", projectFacade.getAllUsers());
        model.addAttribute("statuses", ProjectStatus.values());
        model.addAttribute("types", ProjectType.values());
        model.addAttribute("isNew", isNew);
    }
    
    /**
     * Adds a success message to the redirect attributes
     */
    private void addSuccessMessage(RedirectAttributes redirectAttributes, String messageKey) {
        String message = messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
    }
}