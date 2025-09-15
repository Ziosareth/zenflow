package it.zenflow.controller;

import it.zenflow.dto.MilestoneDTO;
import it.zenflow.facade.MilestoneFacade;
import it.zenflow.model.project.enums.MilestoneStatus;
import it.zenflow.model.rbac.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects/{projectId}/milestones")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
class MilestoneController {

    private final MilestoneFacade milestoneFacade;

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_MILESTONE')")
    public String newMilestoneForm(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = milestoneFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return milestoneFacade.getProjectById(projectId)
                .map(project -> {
                    // Check if user is owner or team member or admin
                    if (!milestoneFacade.isProjectOwner(project, currentUser) &&
                        !milestoneFacade.isTeamMember(project, currentUser) &&
                        userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    MilestoneDTO dto = new MilestoneDTO();
                    dto.setProjectId(projectId);

                    model.addAttribute("project", project);
                    model.addAttribute("milestoneDTO", dto);
                    model.addAttribute("statuses", MilestoneStatus.values());
                    model.addAttribute("isNew", true);
                    return "projects/milestones/form";
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_MILESTONE')")
    public String createMilestone(
            @PathVariable Long projectId,
            @Valid @ModelAttribute MilestoneDTO milestoneDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = milestoneFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            return milestoneFacade.getProjectById(projectId)
                    .map(project -> {
                        model.addAttribute("project", project);
                        model.addAttribute("statuses", MilestoneStatus.values());
                        model.addAttribute("isNew", true);
                        return "projects/milestones/form";
                    })
                    .orElse("redirect:/projects");
        }

        try {
            milestoneFacade.createMilestone(milestoneDTO, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", milestoneFacade.getLocalizedMessage("milestone.created"));
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        }
    }
}
