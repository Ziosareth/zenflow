package it.zenflow.controller;

import it.zenflow.dto.UserStoryDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.ProjectService;
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
@RequestMapping("/projects/{projectId}/user-stories")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class UserStoryController {

    private final UserStoryService userStoryService;
    private final ProjectService projectService;
    private final UserService userService;
    private final MessageSource messageSource;

    @GetMapping("")
    @PreAuthorize("hasAuthority('READ_USER_STORY')")
    public String listUserStories(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        
        return projectService.findById(projectId)
                .map(project -> {
                    List<UserStory> userStories = userStoryService.findByProject(project);
                    
                    model.addAttribute("project", project);
                    model.addAttribute("userStories", userStories);
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("isOwner", project.getOwner().getId().equals(currentUser.getId()));
                    model.addAttribute("isTeamMember", project.getTeamMembers().contains(currentUser));
                    
                    return "projects/user-stories/list";
                })
                .orElse("redirect:/projects");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_USER_STORY')")
    public String viewUserStory(@PathVariable Long projectId, @PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        
        return projectService.findById(projectId)
                .map(project -> userStoryService.findById(id)
                        .map(userStory -> {
                            if (!userStory.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }
                            
                            model.addAttribute("project", project);
                            model.addAttribute("userStory", userStory);
                            model.addAttribute("currentUser", currentUser);
                            model.addAttribute("isOwner", project.getOwner().getId().equals(currentUser.getId()));
                            model.addAttribute("isTeamMember", project.getTeamMembers().contains(currentUser));
                            
                            return "projects/user-stories/detail";
                        })
                        .orElse("redirect:/projects/" + projectId + "/user-stories"))
                .orElse("redirect:/projects");
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_USER_STORY')")
    public String newUserStoryForm(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        
        return projectService.findById(projectId)
                .map(project -> {
                    // Check if user is owner or team member
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !project.getTeamMembers().contains(currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }
                    
                    UserStoryDTO userStoryDTO = new UserStoryDTO();
                    userStoryDTO.setProjectId(projectId);
                    
                    model.addAttribute("project", project);
                    model.addAttribute("userStoryDTO", userStoryDTO);
                    model.addAttribute("statuses", StoryStatus.values());
                    model.addAttribute("priorities", Priority.values());
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
        
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        
        return projectService.findById(projectId)
                .map(project -> {
                    // Check if user is owner or team member
                    if (!project.getOwner().getId().equals(currentUser.getId()) && 
                        !project.getTeamMembers().contains(currentUser) &&
                        !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }
                    
                    if (bindingResult.hasErrors()) {
                        model.addAttribute("project", project);
                        model.addAttribute("statuses", StoryStatus.values());
                        model.addAttribute("priorities", Priority.values());
                        model.addAttribute("teamMembers", project.getTeamMembers());
                        model.addAttribute("isNew", true);
                        return "projects/user-stories/form";
                    }
                    
                    UserStory userStory = new UserStory();
                    userStory.setTitle(userStoryDTO.getTitle());
                    userStory.setDescription(userStoryDTO.getDescription());
                    userStory.setAcceptanceCriteria(userStoryDTO.getAcceptanceCriteria());
                    userStory.setStatus(userStoryDTO.getStatus());
                    userStory.setPriority(userStoryDTO.getPriority());
                    userStory.setStoryPoints(userStoryDTO.getStoryPoints());
                    userStory.setBusinessValue(userStoryDTO.getBusinessValue());
                    userStory.setProject(project);
                    
                    // Set assigned user if provided
                    if (userStoryDTO.getAssignedToId() != null) {
                        userService.findById(userStoryDTO.getAssignedToId())
                                .ifPresent(userStory::setAssignedTo);
                    }
                    
                    // Set PERT estimates if provided
                    userStory.setOptimisticEstimate(userStoryDTO.getOptimisticEstimate());
                    userStory.setPessimisticEstimate(userStoryDTO.getPessimisticEstimate());
                    userStory.setMostLikelyEstimate(userStoryDTO.getMostLikelyEstimate());
                    
                    userStoryService.save(userStory);
                    
                    String message = messageSource.getMessage("userstory.created", null, LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", message);
                    
                    return "redirect:/projects/" + projectId + "/user-stories";
                })
                .orElse("redirect:/projects");
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('UPDATE_USER_STORY')")
    public String editUserStoryForm(
            @PathVariable Long projectId,
            @PathVariable Long id,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        
        return projectService.findById(projectId)
                .map(project -> userStoryService.findById(id)
                        .map(userStory -> {
                            if (!userStory.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }
                            
                            // Check if user is owner or team member
                            if (!project.getOwner().getId().equals(currentUser.getId()) && 
                                !project.getTeamMembers().contains(currentUser) &&
                                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }
                            
                            UserStoryDTO userStoryDTO = new UserStoryDTO();
                            userStoryDTO.setId(userStory.getId());
                            userStoryDTO.setTitle(userStory.getTitle());
                            userStoryDTO.setDescription(userStory.getDescription());
                            userStoryDTO.setAcceptanceCriteria(userStory.getAcceptanceCriteria());
                            userStoryDTO.setStatus(userStory.getStatus());
                            userStoryDTO.setPriority(userStory.getPriority());
                            userStoryDTO.setStoryPoints(userStory.getStoryPoints());
                            userStoryDTO.setBusinessValue(userStory.getBusinessValue());
                            userStoryDTO.setProjectId(projectId);
                            
                            if (userStory.getAssignedTo() != null) {
                                userStoryDTO.setAssignedToId(userStory.getAssignedTo().getId());
                            }
                            
                            userStoryDTO.setOptimisticEstimate(userStory.getOptimisticEstimate());
                            userStoryDTO.setPessimisticEstimate(userStory.getPessimisticEstimate());
                            userStoryDTO.setMostLikelyEstimate(userStory.getMostLikelyEstimate());
                            userStoryDTO.setPertEstimate(userStory.getPertEstimate());
                            
                            model.addAttribute("project", project);
                            model.addAttribute("userStoryDTO", userStoryDTO);
                            model.addAttribute("statuses", StoryStatus.values());
                            model.addAttribute("priorities", Priority.values());
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
        
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        
        return projectService.findById(projectId)
                .map(project -> userStoryService.findById(id)
                        .map(userStory -> {
                            if (!userStory.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }
                            
                            // Check if user is owner or team member
                            if (!project.getOwner().getId().equals(currentUser.getId()) && 
                                !project.getTeamMembers().contains(currentUser) &&
                                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }
                            
                            if (bindingResult.hasErrors()) {
                                model.addAttribute("project", project);
                                model.addAttribute("statuses", StoryStatus.values());
                                model.addAttribute("priorities", Priority.values());
                                model.addAttribute("teamMembers", project.getTeamMembers());
                                model.addAttribute("isNew", false);
                                return "projects/user-stories/form";
                            }
                            
                            userStory.setTitle(userStoryDTO.getTitle());
                            userStory.setDescription(userStoryDTO.getDescription());
                            userStory.setAcceptanceCriteria(userStoryDTO.getAcceptanceCriteria());
                            userStory.setStatus(userStoryDTO.getStatus());
                            userStory.setPriority(userStoryDTO.getPriority());
                            userStory.setStoryPoints(userStoryDTO.getStoryPoints());
                            userStory.setBusinessValue(userStoryDTO.getBusinessValue());
                            
                            // Update assigned user if provided
                            if (userStoryDTO.getAssignedToId() != null) {
                                userService.findById(userStoryDTO.getAssignedToId())
                                        .ifPresent(userStory::setAssignedTo);
                            } else {
                                userStory.setAssignedTo(null);
                            }
                            
                            // Update PERT estimates if provided
                            userStory.setOptimisticEstimate(userStoryDTO.getOptimisticEstimate());
                            userStory.setPessimisticEstimate(userStoryDTO.getPessimisticEstimate());
                            userStory.setMostLikelyEstimate(userStoryDTO.getMostLikelyEstimate());
                            
                            userStoryService.save(userStory);
                            
                            String message = messageSource.getMessage("userstory.updated", null, LocaleContextHolder.getLocale());
                            redirectAttributes.addFlashAttribute("message", message);
                            
                            return "redirect:/projects/" + projectId + "/user-stories/" + id;
                        })
                        .orElse("redirect:/projects/" + projectId + "/user-stories"))
                .orElse("redirect:/projects");
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('DELETE_USER_STORY')")
    public String deleteUserStory(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        
        return projectService.findById(projectId)
                .map(project -> userStoryService.findById(id)
                        .map(userStory -> {
                            if (!userStory.getProject().getId().equals(projectId)) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }
                            
                            // Check if user is owner or has admin rights
                            if (!project.getOwner().getId().equals(currentUser.getId()) && 
                                !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                                return "redirect:/projects/" + projectId + "/user-stories";
                            }
                            
                            userStoryService.deleteById(id);
                            
                            String message = messageSource.getMessage("userstory.deleted", null, LocaleContextHolder.getLocale());
                            redirectAttributes.addFlashAttribute("message", message);
                            
                            return "redirect:/projects/" + projectId + "/user-stories";
                        })
                        .orElse("redirect:/projects/" + projectId + "/user-stories"))
                .orElse("redirect:/projects");
    }
}