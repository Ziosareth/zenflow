package it.zenflow.controller;

import it.zenflow.dto.UserStoryDTO;
import it.zenflow.facade.UserStoryFacade;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects/{projectId}/user-stories")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class UserStoryController {

    private final UserStoryFacade userStoryFacade;

    @GetMapping("")
    @PreAuthorize("hasAuthority('READ_USER_STORY')")
    public String listUserStories(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = userStoryFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return userStoryFacade.getProjectById(projectId)
                .map(project -> {
                    Page<UserStory> userStoriesPage = userStoryFacade.findByProjectPaginated(
                            project, PageRequest.of(page, size, Sort.by(sort)));
                    
                    model.addAttribute("project", project);
                    model.addAttribute("userStories", userStoriesPage.getContent());
                    model.addAttribute("currentPage", page);
                    model.addAttribute("totalPages", userStoriesPage.getTotalPages());
                    model.addAttribute("totalItems", userStoriesPage.getTotalElements());
                    model.addAttribute("pageSize", size);
                    model.addAttribute("sortField", sort);
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("isOwner", userStoryFacade.isProjectOwner(project, currentUser));
                    model.addAttribute("isTeamMember", userStoryFacade.isTeamMember(project, currentUser));

                    return "projects/user-stories/list";
                })
                .orElse("redirect:/projects");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_USER_STORY')")
    public String viewUserStory(@PathVariable Long projectId, @PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userStoryFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return userStoryFacade.getProjectById(projectId)
                .map(project -> userStoryFacade.findByIdWithTasks(id)
                        .map(userStory -> {
                            if (!userStory.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }

                            model.addAttribute("project", project);
                            model.addAttribute("userStory", userStory);
                            model.addAttribute("currentUser", currentUser);
                            model.addAttribute("isOwner", userStoryFacade.isProjectOwner(project, currentUser));
                            model.addAttribute("isTeamMember", userStoryFacade.isTeamMember(project, currentUser));

                            return "projects/user-stories/detail";
                        })
                        .orElse("redirect:/projects/" + projectId + "/user-stories"))
                .orElse("redirect:/projects");
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_USER_STORY')")
    public String newUserStoryForm(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userStoryFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return userStoryFacade.getProjectById(projectId)
                .map(project -> {
                    // Check if user is owner or team member
                    if (!userStoryFacade.isProjectOwner(project, currentUser) && 
                        !userStoryFacade.isTeamMember(project, currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    UserStoryDTO userStoryDTO = new UserStoryDTO();
                    userStoryDTO.setProjectId(projectId);

                    model.addAttribute("project", project);
                    model.addAttribute("userStoryDTO", userStoryDTO);
                    model.addAttribute("statuses", StoryStatus.values());
                    model.addAttribute("priorities", Priority.values());
                    model.addAttribute("estimationTypes", it.zenflow.model.project.enums.EstimationType.values());
                    model.addAttribute("teamMembers", project.getTeamMembers());
                    model.addAttribute("isNew", true);

                    return "projects/user-stories/form";
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_USER_STORY')")
    public String createUserStory(
            @PathVariable Long projectId,
            @Valid @ModelAttribute UserStoryDTO userStoryDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = userStoryFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            return userStoryFacade.getProjectById(projectId)
                    .map(project -> {
                        model.addAttribute("project", project);
                        model.addAttribute("statuses", StoryStatus.values());
                        model.addAttribute("priorities", Priority.values());
                        model.addAttribute("estimationTypes", it.zenflow.model.project.enums.EstimationType.values());
                        model.addAttribute("teamMembers", project.getTeamMembers());
                        model.addAttribute("isNew", true);
                        return "projects/user-stories/form";
                    })
                    .orElse("redirect:/projects");
        }

        try {
            userStoryFacade.createUserStory(userStoryDTO, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", userStoryFacade.getLocalizedMessage("userstory.created"));
            return "redirect:/projects/" + projectId + "/user-stories";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/user-stories";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_USER_STORY')")
    public String editUserStoryForm(
            @PathVariable Long projectId,
            @PathVariable Long id,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userStoryFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return userStoryFacade.getProjectById(projectId)
                .map(project -> userStoryFacade.findById(id)
                        .map(userStory -> {
                            if (!userStory.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }

                            // Check if user is owner or team member
                            if (!userStoryFacade.isProjectOwner(project, currentUser) && 
                                !userStoryFacade.isTeamMember(project, currentUser) &&
                                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }

                            UserStoryDTO userStoryDTO = userStoryFacade.mapToDTO(userStory);

                            model.addAttribute("project", project);
                            model.addAttribute("userStoryDTO", userStoryDTO);
                            model.addAttribute("statuses", StoryStatus.values());
                            model.addAttribute("priorities", Priority.values());
                            model.addAttribute("estimationTypes", it.zenflow.model.project.enums.EstimationType.values());
                            model.addAttribute("teamMembers", project.getTeamMembers());
                            model.addAttribute("isNew", false);

                            return "projects/user-stories/form";
                        })
                        .orElse("redirect:/projects/" + projectId + "/user-stories"))
                .orElse("redirect:/projects");
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_USER_STORY')")
    public String updateUserStory(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @ModelAttribute UserStoryDTO userStoryDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = userStoryFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            return userStoryFacade.getProjectById(projectId)
                    .map(project -> {
                        model.addAttribute("project", project);
                        model.addAttribute("statuses", StoryStatus.values());
                        model.addAttribute("priorities", Priority.values());
                        model.addAttribute("estimationTypes", it.zenflow.model.project.enums.EstimationType.values());
                        model.addAttribute("teamMembers", project.getTeamMembers());
                        model.addAttribute("isNew", false);
                        return "projects/user-stories/form";
                    })
                    .orElse("redirect:/projects");
        }

        try {
            userStoryFacade.updateUserStory(id, userStoryDTO, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", userStoryFacade.getLocalizedMessage("userstory.updated"));
            return "redirect:/projects/" + projectId + "/user-stories/" + id;
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/user-stories";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId + "/user-stories";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('DELETE_USER_STORY')")
    public String deleteUserStory(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User currentUser = userStoryFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            userStoryFacade.deleteUserStory(projectId, id, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", userStoryFacade.getLocalizedMessage("userstory.deleted"));
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/projects/" + projectId + "/user-stories";
    }
}