package it.zenflow.controller;

import it.zenflow.dto.*;
import it.zenflow.model.project.EstimationVote;
import it.zenflow.model.project.PlanningPokerSession;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.EstimationType;
import it.zenflow.model.project.enums.SessionStatus;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.EstimationVoteService;
import it.zenflow.service.PlanningPokerSessionService;
import it.zenflow.service.ProjectService;
import it.zenflow.service.UserStoryService;
import it.zenflow.service.rbac.UserService;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/planning-poker")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PlanningPokerController {

    private final PlanningPokerSessionService planningPokerSessionService;
    private final EstimationVoteService estimationVoteService;
    private final ProjectService projectService;
    private final UserStoryService userStoryService;
    private final UserService userService;
    private final MessageSource messageSource;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PLANNING_POKER_SESSION')")
    public String listSessions(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
        List<PlanningPokerSession> sessions = planningPokerSessionService.findAll();

        model.addAttribute("sessions", sessions);
        model.addAttribute("currentUser", currentUser);

        return "planning-poker/list";
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('READ_PLANNING_POKER_SESSION')")
    public String listProjectSessions(@PathVariable Long projectId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return projectService.findById(projectId)
                .map(project -> {
                    List<PlanningPokerSession> sessions = planningPokerSessionService.findByProjectId(projectId);

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
    public String newSessionForm(Model model, @RequestParam(required = false) Long projectId, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        CreatePlanningPokerSessionCommand command = new CreatePlanningPokerSessionCommand();
        if (projectId != null) {
            command.setProjectId(projectId);
        }

        model.addAttribute("command", command);
        model.addAttribute("projects", projectService.findAll());
        model.addAttribute("estimationTypes", EstimationType.values());
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
            model.addAttribute("projects", projectService.findAll());
            model.addAttribute("estimationTypes", EstimationType.values());
            return "planning-poker/new";
        }

        User facilitator = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        PlanningPokerSession session = planningPokerSessionService.createSession(command, facilitator);

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
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return planningPokerSessionService.findByIdWithParticipantsAndVotes(id)
                .map(session -> {
                    Project project = session.getProject();
                    List<EstimationVote> votes = estimationVoteService.findBySessionIdWithVoter(id);

                    model.addAttribute("pokerSession", session);
                    model.addAttribute("project", project);
                    model.addAttribute("votes", votes);
                    model.addAttribute("currentUser", currentUser);
                    model.addAttribute("isFacilitator", session.getFacilitator().getId().equals(currentUser.getId()));
                    model.addAttribute("isParticipant", session.getParticipants().contains(currentUser));

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

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return planningPokerSessionService.findById(id)
                .map(session -> {
                    // Only facilitator can start the session
                    if (!session.getFacilitator().getId().equals(currentUser.getId())) {
                        String errorMessage = messageSource.getMessage(
                                "planningpoker.error.not.facilitator", 
                                null, 
                                "Only the facilitator can start the session", 
                                LocaleContextHolder.getLocale());
                        redirectAttributes.addFlashAttribute("error", errorMessage);
                        return "redirect:/planning-poker/" + id;
                    }

                    try {
                        planningPokerSessionService.startSession(id);
                        String successMessage = messageSource.getMessage(
                                "planningpoker.started", 
                                null, 
                                "Planning poker session started successfully", 
                                LocaleContextHolder.getLocale());
                        redirectAttributes.addFlashAttribute("message", successMessage);
                    } catch (IllegalStateException e) {
                        redirectAttributes.addFlashAttribute("error", e.getMessage());
                    }

                    return "redirect:/planning-poker/" + id;
                })
                .orElse("redirect:/planning-poker");
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('UPDATE_PLANNING_POKER_SESSION')")
    public String completeSession(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return planningPokerSessionService.findById(id)
                .map(session -> {
                    // Only facilitator can complete the session
                    if (!session.getFacilitator().getId().equals(currentUser.getId())) {
                        String errorMessage = messageSource.getMessage(
                                "planningpoker.error.not.facilitator", 
                                null, 
                                "Only the facilitator can complete the session", 
                                LocaleContextHolder.getLocale());
                        redirectAttributes.addFlashAttribute("error", errorMessage);
                        return "redirect:/planning-poker/" + id;
                    }

                    try {
                        planningPokerSessionService.completeSession(id);
                        String successMessage = messageSource.getMessage(
                                "planningpoker.completed", 
                                null, 
                                "Planning poker session completed successfully", 
                                LocaleContextHolder.getLocale());
                        redirectAttributes.addFlashAttribute("message", successMessage);
                    } catch (IllegalStateException e) {
                        redirectAttributes.addFlashAttribute("error", e.getMessage());
                    }

                    return "redirect:/planning-poker/" + id;
                })
                .orElse("redirect:/planning-poker");
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('UPDATE_PLANNING_POKER_SESSION')")
    public String cancelSession(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return planningPokerSessionService.findById(id)
                .map(session -> {
                    // Only facilitator can cancel the session
                    if (!session.getFacilitator().getId().equals(currentUser.getId())) {
                        String errorMessage = messageSource.getMessage(
                                "planningpoker.error.not.facilitator", 
                                null, 
                                "Only the facilitator can cancel the session", 
                                LocaleContextHolder.getLocale());
                        redirectAttributes.addFlashAttribute("error", errorMessage);
                        return "redirect:/planning-poker/" + id;
                    }

                    planningPokerSessionService.cancelSession(id);
                    String successMessage = messageSource.getMessage(
                            "planningpoker.cancelled", 
                            null, 
                            "Planning poker session cancelled", 
                            LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", successMessage);

                    return "redirect:/planning-poker/" + id;
                })
                .orElse("redirect:/planning-poker");
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('DELETE_PLANNING_POKER_SESSION')")
    public String deleteSession(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return planningPokerSessionService.findById(id)
                .map(session -> {
                    // Only facilitator can delete the session
                    if (!session.getFacilitator().getId().equals(currentUser.getId()) &&
                            userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ADMIN"))) {
                        String errorMessage = messageSource.getMessage(
                                "planningpoker.error.not.facilitator", 
                                null, 
                                "Only the facilitator can delete the session", 
                                LocaleContextHolder.getLocale());
                        redirectAttributes.addFlashAttribute("error", errorMessage);
                        return "redirect:/planning-poker/" + id;
                    }

                    Long projectId = session.getProject().getId();
                    planningPokerSessionService.deleteSession(id);
                    String successMessage = messageSource.getMessage(
                            "planningpoker.deleted", 
                            null, 
                            "Planning poker session deleted", 
                            LocaleContextHolder.getLocale());
                    redirectAttributes.addFlashAttribute("message", successMessage);

                    return "redirect:/planning-poker/project/" + projectId;
                })
                .orElse("redirect:/planning-poker");
    }

    @GetMapping("/{id}/vote/{userStoryId}")
    @PreAuthorize("hasAuthority('CREATE_ESTIMATION_VOTE')")
    public String voteForm(
            @PathVariable Long id,
            @PathVariable Long userStoryId,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();

        return planningPokerSessionService.findByIdWithParticipants(id)
                .map(session -> userStoryService.findById(userStoryId)
                        .map(userStory -> {
                            // Check if user is a participant
                            if (!session.getParticipants().contains(currentUser) && 
                                !session.getFacilitator().getId().equals(currentUser.getId())) {
                                return "redirect:/planning-poker/" + id;
                            }

                            // Check if session is active
                            if (session.getStatus() != SessionStatus.ACTIVE) {
                                return "redirect:/planning-poker/" + id;
                            }

                            EstimationVoteDTO voteDTO = new EstimationVoteDTO();
                            voteDTO.setSessionId(id);
                            voteDTO.setUserStoryId(userStoryId);
                            voteDTO.setVoterId(currentUser.getId());

                            // Check if user has already voted
                            estimationVoteService.findBySessionAndUserStoryAndVoter(session, userStory, currentUser)
                                    .ifPresent(vote -> {
                                        voteDTO.setId(vote.getId());
                                        voteDTO.setStoryPoints(vote.getStoryPoints());
                                        voteDTO.setReasoning(vote.getReasoning());
                                    });

                            model.addAttribute("pokerSession", session);
                            model.addAttribute("userStory", userStory);
                            model.addAttribute("vote", voteDTO);
                            model.addAttribute("currentUser", currentUser);

                            return "planning-poker/vote";
                        })
                        .orElse("redirect:/planning-poker/" + id))
                .orElse("redirect:/planning-poker");
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
            User currentUser = userService.findByUsername(userDetails.getUsername()).orElseThrow();
            PlanningPokerSession session = planningPokerSessionService.findByIdWithParticipants(id).orElseThrow();
            UserStory userStory = userStoryService.findById(userStoryId).orElseThrow();

            model.addAttribute("session", session);
            model.addAttribute("userStory", userStory);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("vote", voteDTO);

            return "planning-poker/vote";
        }

        User voter = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        try {
            estimationVoteService.saveVote(
                    id,
                    userStoryId,
                    voter,
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
        List<UserStory> stories = userStoryService.findByProjectIdAndStatus(projectId, StoryStatus.BACKLOG);
        return stories.stream()
                .map(this::convertToUserStoryDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/project/{projectId}/members")
    @PreAuthorize("hasAuthority('READ_PROJECT')")
    @ResponseBody
    public List<UserDTO> getProjectMembers(@PathVariable Long projectId) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        return project.getTeamMembers().stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    private UserDTO convertToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setEnabled(user.isEnabled());
        dto.setTenant(user.getTenant());
        return dto;
    }

    private UserStoryDTO convertToUserStoryDTO(UserStory userStory) {
        UserStoryDTO dto = new UserStoryDTO();
        dto.setId(userStory.getId());
        dto.setTitle(userStory.getTitle());
        dto.setDescription(userStory.getDescription());
        dto.setStatus(userStory.getStatus());
        dto.setPriority(userStory.getPriority());
        dto.setStoryPoints(userStory.getStoryPoints());
        dto.setProjectId(userStory.getProject().getId());
        dto.setEstimationType(userStory.getEstimationType());
        return dto;
    }
}
