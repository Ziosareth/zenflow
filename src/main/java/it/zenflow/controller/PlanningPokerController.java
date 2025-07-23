package it.zenflow.controller;

import it.zenflow.dto.CreatePlanningPokerSessionCommand;
import it.zenflow.dto.EstimationVoteDTO;
import it.zenflow.dto.UserDTO;
import it.zenflow.dto.UserStoryDTO;
import it.zenflow.facade.PlanningPokerFacade;
import it.zenflow.model.project.EstimationVote;
import it.zenflow.model.project.PlanningPokerSession;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.SessionStatus;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/planning-poker")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PlanningPokerController {

    private final PlanningPokerFacade planningPokerFacade;
    private final MessageSource messageSource;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PLANNING_POKER_SESSION')")
    public String listSessions(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();
        List<PlanningPokerSession> sessions = planningPokerFacade.getAllSessions();

        model.addAttribute("sessions", sessions);
        model.addAttribute("currentUser", currentUser);

        return "planning-poker/list";
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('READ_PLANNING_POKER_SESSION')")
    public String listProjectSessions(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return planningPokerFacade.getProjectById(projectId)
                .map(project -> {
                    List<PlanningPokerSession> sessions = planningPokerFacade.getSessionsByProjectId(projectId);

                    model.addAttribute("project", project);
                    model.addAttribute("sessions", sessions);
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("isOwner", project.getOwner().getId().equals(currentUser.getId()));
                    model.addAttribute("isTeamMember", project.getTeamMembers().contains(currentUser));

                    return "planning-poker/project-list";
                })
                .orElse("redirect:/projects");
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_PLANNING_POKER_SESSION')")
    public String newSessionForm(Model model, 
                            @RequestParam(required = false) Long projectId, 
                            @RequestParam(required = false) Long userStoryId, 
                            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        CreatePlanningPokerSessionCommand command = new CreatePlanningPokerSessionCommand();
        if (projectId != null) {
            command.setProjectId(projectId);
        }
        if (userStoryId != null) {
            command.setUserStoryId(userStoryId);
        }

        model.addAttribute("command", command);
        model.addAttribute("projects", planningPokerFacade.getAllProjects());
        model.addAttribute("currentUser", currentUser);

        return "planning-poker/new";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('CREATE_PLANNING_POKER_SESSION')")
    public String createSession(
            @Valid @ModelAttribute("command") CreatePlanningPokerSessionCommand command,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("projects", planningPokerFacade.getAllProjects());
            return "planning-poker/new";
        }

        User facilitator = planningPokerFacade.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        PlanningPokerSession session = planningPokerFacade.createSession(command, facilitator);

        String successMessage = messageSource.getMessage(
                "planningpoker.created", 
                null, 
                "Planning poker session created successfully", 
                LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", successMessage);

        return "redirect:/planning-poker/" + session.getId();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_PLANNING_POKER_SESSION')")
    public String viewSession(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        return planningPokerFacade.getSessionByIdWithParticipantsAndVotes(id)
                .map(session -> {
                    Project project = session.getProject();
                    List<EstimationVote> votes = planningPokerFacade.getVotesBySessionId(id);

                    model.addAttribute("pokerSession", session);
                    model.addAttribute("project", project);
                    model.addAttribute("votes", votes);
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("isFacilitator", planningPokerFacade.isFacilitator(session, currentUser));
                    model.addAttribute("isParticipant", planningPokerFacade.isParticipant(session, currentUser));

                    // Add user story and votes to the model
                    model.addAttribute("userStory", session.getUserStory());
                    model.addAttribute("userStoryVotes", votes);

                    return "planning-poker/view";
                })
                .orElse("redirect:/planning-poker");
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('UPDATE_PLANNING_POKER_SESSION')")
    public String startSession(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            planningPokerFacade.startSessionWithAuthorization(id, currentUser);
            String successMessage = messageSource.getMessage(
                    "planningpoker.started", 
                    null, 
                    "Planning poker session started successfully", 
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (AccessDeniedException e) {
            String errorMessage = messageSource.getMessage(
                    "planningpoker.error.not.facilitator", 
                    null, 
                    "Only the facilitator can start the session", 
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/planning-poker/" + id;
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('UPDATE_PLANNING_POKER_SESSION')")
    public String completeSession(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            planningPokerFacade.completeSessionWithAuthorization(id, currentUser);
            String successMessage = messageSource.getMessage(
                    "planningpoker.completed", 
                    null, 
                    "Planning poker session completed successfully", 
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (AccessDeniedException e) {
            String errorMessage = messageSource.getMessage(
                    "planningpoker.error.not.facilitator", 
                    null, 
                    "Only the facilitator can complete the session", 
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/planning-poker/" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('UPDATE_PLANNING_POKER_SESSION')")
    public String cancelSession(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            planningPokerFacade.cancelSessionWithAuthorization(id, currentUser);
            String successMessage = messageSource.getMessage(
                    "planningpoker.cancelled", 
                    null, 
                    "Planning poker session cancelled", 
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (AccessDeniedException e) {
            String errorMessage = messageSource.getMessage(
                    "planningpoker.error.not.facilitator", 
                    null, 
                    "Only the facilitator can cancel the session", 
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/planning-poker/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('DELETE_PLANNING_POKER_SESSION')")
    public String deleteSession(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            Long projectId = planningPokerFacade.deleteSessionWithAuthorization(id, currentUser, userDetails);
            String successMessage = messageSource.getMessage(
                    "planningpoker.deleted", 
                    null, 
                    "Planning poker session deleted", 
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("message", successMessage);
            return "redirect:/planning-poker/project/" + projectId;
        } catch (AccessDeniedException e) {
            String errorMessage = messageSource.getMessage(
                    "planningpoker.error.not.facilitator", 
                    null, 
                    "Only the facilitator can delete the session", 
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/planning-poker/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/planning-poker";
        }
    }

    @GetMapping("/{id}/vote/{userStoryId}")
    @PreAuthorize("hasAuthority('CREATE_ESTIMATION_VOTE')")
    public String voteForm(
            @PathVariable Long id,
            @PathVariable Long userStoryId,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();

        try {
            EstimationVoteDTO voteDTO = planningPokerFacade.prepareVoteDTO(id, userStoryId, currentUser);
            PlanningPokerSession session = planningPokerFacade.getSessionByIdWithParticipants(id).orElseThrow();
            UserStory userStory = planningPokerFacade.getUserStoryById(userStoryId).orElseThrow();
            
            model.addAttribute("pokerSession", session);
            model.addAttribute("userStory", userStory);
            model.addAttribute("vote", voteDTO);
            model.addAttribute("currentUser", currentUser);
            
            return "planning-poker/vote";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to vote in this session");
            return "redirect:/planning-poker/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/planning-poker/" + id;
        }
    }

    @PostMapping("/{id}/vote/{userStoryId}")
    @PreAuthorize("hasAuthority('CREATE_ESTIMATION_VOTE')")
    public String submitVote(
            @PathVariable Long id,
            @PathVariable Long userStoryId,
            @Valid @ModelAttribute("vote") EstimationVoteDTO voteDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            User currentUser = planningPokerFacade.getUserByUsername(userDetails.getUsername()).orElseThrow();
            PlanningPokerSession session = planningPokerFacade.getSessionByIdWithParticipants(id).orElseThrow();
            UserStory userStory = planningPokerFacade.getUserStoryById(userStoryId).orElseThrow();

            model.addAttribute("pokerSession", session);
            model.addAttribute("userStory", userStory);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("vote", voteDTO);

            return "planning-poker/vote";
        }

         try {
            planningPokerFacade.saveVoteByUsername(
                    id,
                    userStoryId,
                    userDetails.getUsername(),
                    voteDTO.getStoryPoints(),
                    voteDTO.getReasoning()
            );

            String successMessage = messageSource.getMessage(
                    "planningpoker.vote.submitted", 
                    null, 
                    "Vote submitted successfully", 
                    LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("message", successMessage);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/planning-poker/" + id;
    }

    @GetMapping("/project/{projectId}/stories")
    @PreAuthorize("hasAuthority('READ_USER_STORY')")
    @ResponseBody
    public List<UserStoryDTO> getProjectStories(@PathVariable Long projectId) {
        List<UserStory> stories = planningPokerFacade.getUserStoriesByProjectIdAndStatus(projectId, StoryStatus.BACKLOG);
        return stories.stream()
                .map(planningPokerFacade::convertToUserStoryDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/project/{projectId}/members")
    @PreAuthorize("hasAuthority('READ_PROJECT')")
    @ResponseBody
    public List<UserDTO> getProjectMembers(@PathVariable Long projectId) {
        Project project = planningPokerFacade.getProjectById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        return project.getTeamMembers().stream()
                .map(planningPokerFacade::convertToUserDTO)
                .collect(Collectors.toList());
    }
}