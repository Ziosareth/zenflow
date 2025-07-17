package it.zenflow.controller;

import it.zenflow.dto.ProjectDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.rbac.User;
import it.zenflow.service.ProjectService;
import it.zenflow.service.rbac.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/projects")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;
    private final MessageSource messageSource;

    @GetMapping("")
    public String listProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        Page<Project> projectPage = projectService.findAllWithOwners(pageable);

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
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(id)
                .map(project -> {
                    model.addAttribute("project", project);
                    model.addAttribute("currentUser", currentUser);

                    // Safely check if the current user is the owner
                    boolean isOwner = project.getOwner() != null && 
                                     project.getOwner().getId() != null && 
                                     project.getOwner().getId().equals(currentUser.getId());
                    model.addAttribute("isOwner", isOwner);

                    // Safely check if the current user is a team member
                    boolean isTeamMember = project.getTeamMembers() != null && 
                                          project.getTeamMembers().contains(currentUser);
                    model.addAttribute("isTeamMember", isTeamMember);

                    return "projects/detail";
                })
                .orElse("redirect:/projects");
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_PROJECT')")
    public String newProjectForm(Model model) {
        model.addAttribute("projectDTO", new ProjectDTO());
        model.addAttribute("allUsers", userService.findAll());
        model.addAttribute("statuses", ProjectStatus.values());
        model.addAttribute("types", ProjectType.values());
        model.addAttribute("isNew", true);
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
            model.addAttribute("allUsers", userService.findAll());
            model.addAttribute("statuses", ProjectStatus.values());
            model.addAttribute("types", ProjectType.values());
            model.addAttribute("isNew", true);
            return "projects/form";
        }

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        Project project = new Project();
        project.setName(projectDTO.getName());
        project.setDescription(projectDTO.getDescription());
        project.setStatus(projectDTO.getStatus());
        project.setType(projectDTO.getType());
        project.setStartDate(projectDTO.getStartDate());
        project.setEndDate(projectDTO.getEndDate());
        project.setOwner(currentUser);

        // Add team members
        Set<User> teamMembers = new HashSet<>();
        if (projectDTO.getTeamMemberIds() != null && !projectDTO.getTeamMemberIds().isEmpty()) {
            for (Long userId : projectDTO.getTeamMemberIds()) {
                userService.findById(userId).ifPresent(teamMembers::add);
            }
        }
        project.setTeamMembers(teamMembers);

        projectService.save(project);

        String message = messageSource.getMessage("project.created", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);

        return "redirect:/projects";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_PROJECT')")
    public String editProjectForm(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(id)
                .map(project -> {
                    // Check if user is owner or has admin rights
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects";
                    }

                    ProjectDTO projectDTO = new ProjectDTO();
                    projectDTO.setId(project.getId());
                    projectDTO.setName(project.getName());
                    projectDTO.setDescription(project.getDescription());
                    projectDTO.setStatus(project.getStatus());
                    projectDTO.setType(project.getType());
                    projectDTO.setStartDate(project.getStartDate());
                    projectDTO.setEndDate(project.getEndDate());

                    // Set team member IDs
                    Set<Long> teamMemberIds = project.getTeamMembers().stream()
                            .map(User::getId)
                            .collect(Collectors.toSet());
                    projectDTO.setTeamMemberIds(teamMemberIds);

                    model.addAttribute("projectDTO", projectDTO);
                    model.addAttribute("allUsers", userService.findAll());
                    model.addAttribute("statuses", ProjectStatus.values());
                    model.addAttribute("types", ProjectType.values());
                    model.addAttribute("isNew", false);

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
            model.addAttribute("allUsers", userService.findAll());
            model.addAttribute("statuses", ProjectStatus.values());
            model.addAttribute("types", ProjectType.values());
            model.addAttribute("isNew", false);
            return "projects/form";
        }

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(id)
                .map(project -> {
                    // Check if user is owner or has admin rights
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects";
                    }

                    project.setName(projectDTO.getName());
                    project.setDescription(projectDTO.getDescription());
                    project.setStatus(projectDTO.getStatus());
                    project.setType(projectDTO.getType());
                    project.setStartDate(projectDTO.getStartDate());
                    project.setEndDate(projectDTO.getEndDate());

                    // Update team members
                    Set<User> teamMembers = new HashSet<>();
                    if (projectDTO.getTeamMemberIds() != null && !projectDTO.getTeamMemberIds().isEmpty()) {
                        for (Long userId : projectDTO.getTeamMemberIds()) {
                            userService.findById(userId).ifPresent(teamMembers::add);
                        }
                    }
                    project.setTeamMembers(teamMembers);

                    projectService.save(project);

                    String message = messageSource.getMessage("project.updated", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);

                    return "redirect:/projects/" + id;
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('DELETE_PROJECT')")
    public String deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(id)
                .map(project -> {
                    // Check if user is owner or has admin rights
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects";
                    }

                    projectService.deleteById(id);

                    String message = messageSource.getMessage("project.deleted", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);

                    return "redirect:/projects";
                })
                .orElse("redirect:/projects");
    }

    @GetMapping("/my")
    public String myProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        List<Project> projects = projectService.findByOwner(currentUser);

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
}
