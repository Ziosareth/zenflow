package it.zenflow.controller;

import it.zenflow.dto.EpicDTO;
import it.zenflow.facade.EpicFacade;
import it.zenflow.model.project.enums.EpicStatus;
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
@RequestMapping("/projects/{projectId}/epics")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
class EpicController {

    private final EpicFacade epicFacade;

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_EPIC')")
    public String newEpicForm(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = epicFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return epicFacade.getProjectById(projectId)
                .map(project -> {
                    // Check if user is owner or team member or admin
                    if (!epicFacade.isProjectOwner(project, currentUser) &&
                        !epicFacade.isTeamMember(project, currentUser) &&
                        userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        return "redirect:/projects/" + projectId;
                    }

                    EpicDTO dto = new EpicDTO();
                    dto.setProjectId(projectId);

                    model.addAttribute("project", project);
                    model.addAttribute("epicDTO", dto);
                    model.addAttribute("statuses", EpicStatus.values());
                    model.addAttribute("isNew", true);
                    return "projects/epics/form";
                })
                .orElse("redirect:/projects");
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_EPIC')")
    public String createEpic(
            @PathVariable Long projectId,
            @Valid @ModelAttribute EpicDTO epicDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = epicFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        if (bindingResult.hasErrors()) {
            return epicFacade.getProjectById(projectId)
                    .map(project -> {
                        model.addAttribute("project", project);
                        model.addAttribute("statuses", EpicStatus.values());
                        model.addAttribute("isNew", true);
                        return "projects/epics/form";
                    })
                    .orElse("redirect:/projects");
        }

        try {
            epicFacade.createEpic(epicDTO, currentUser, userDetails);
            redirectAttributes.addFlashAttribute("message", epicFacade.getLocalizedMessage("epic.created"));
            return "redirect:/projects/" + projectId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/projects/" + projectId;
        }
    }
}
