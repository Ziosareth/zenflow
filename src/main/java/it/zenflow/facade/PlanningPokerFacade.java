package it.zenflow.facade;

import it.zenflow.dto.CreatePlanningPokerSessionCommand;
import it.zenflow.dto.EstimationVoteDTO;
import it.zenflow.dto.UserDTO;
import it.zenflow.dto.UserStoryDTO;
import it.zenflow.mapper.UserMapper;
import it.zenflow.mapper.UserStoryMapper;
import it.zenflow.model.project.EstimationVote;
import it.zenflow.model.project.PlanningPokerSession;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.SessionStatus;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.EstimationVoteService;
import it.zenflow.service.PlanningPokerSessionService;
import it.zenflow.service.ProjectService;
import it.zenflow.service.UserStoryService;
import it.zenflow.service.rbac.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Facade for Planning Poker-related business operations.
 * This class encapsulates the business logic for planning poker session management.
 */
@Component
@RequiredArgsConstructor
public class PlanningPokerFacade {
    private final PlanningPokerSessionService planningPokerSessionService;
    private final EstimationVoteService estimationVoteService;
    private final ProjectService projectService;
    private final UserStoryService userStoryService;
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserStoryMapper userStoryMapper;

    /**
     * Retrieves all planning poker sessions
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<PlanningPokerSession> getAllSessions() {
        return planningPokerSessionService.findAll();
    }

    /**
     * Retrieves planning poker sessions for a specific project
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<PlanningPokerSession> getSessionsByProjectId(Long projectId) {
        return planningPokerSessionService.findByProjectId(projectId);
    }

    /**
     * Retrieves a specific planning poker session by ID with participants and votes
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<PlanningPokerSession> getSessionByIdWithParticipantsAndVotes(Long id) {
        return planningPokerSessionService.findByIdWithParticipantsAndVotes(id);
    }

    /**
     * Retrieves a specific planning poker session by ID with participants
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<PlanningPokerSession> getSessionByIdWithParticipants(Long id) {
        return planningPokerSessionService.findByIdWithParticipants(id);
    }

    /**
     * Retrieves a specific planning poker session by ID
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<PlanningPokerSession> getSessionById(Long id) {
        return planningPokerSessionService.findById(id);
    }

    /**
     * Creates a new planning poker session
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public PlanningPokerSession createSession(CreatePlanningPokerSessionCommand command, User facilitator) {
        return planningPokerSessionService.createSession(command, facilitator);
    }

    /**
     * Starts a planning poker session
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void startSession(Long id) {
        planningPokerSessionService.startSession(id);
    }

    /**
     * Completes a planning poker session
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void completeSession(Long id) {
        planningPokerSessionService.completeSession(id);
    }

    /**
     * Cancels a planning poker session
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void cancelSession(Long id) {
        planningPokerSessionService.cancelSession(id);
    }

    /**
     * Deletes a planning poker session
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteSession(Long id) {
        planningPokerSessionService.deleteSession(id);
    }

    /**
     * Retrieves votes for a specific session
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<EstimationVote> getVotesBySessionId(Long sessionId) {
        return estimationVoteService.findBySessionIdWithVoter(sessionId);
    }

    /**
     * Finds a vote by session, user story, and voter
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<EstimationVote> findVoteBySessionAndUserStoryAndVoter(
            PlanningPokerSession session, UserStory userStory, User voter) {
        return estimationVoteService.findBySessionAndUserStoryAndVoter(session, userStory, voter);
    }

    /**
     * Saves a vote
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public EstimationVote saveVote(Long sessionId, Long userStoryId, User voter, Integer storyPoints, String reasoning) {
        return estimationVoteService.saveVote(sessionId, userStoryId, voter, storyPoints, reasoning);
    }

    /**
     * Retrieves a project by ID
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Project> getProjectById(Long id) {
        return projectService.findById(id);
    }

    /**
     * Retrieves all projects
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Project> getAllProjects() {
        return projectService.findAll();
    }

    /**
     * Retrieves user stories by project ID and status
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> getUserStoriesByProjectIdAndStatus(Long projectId, StoryStatus status) {
        return userStoryService.findByProjectIdAndStatus(projectId, status);
    }

    /**
     * Retrieves a user story by ID
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<UserStory> getUserStoryById(Long id) {
        return userStoryService.findById(id);
    }

    /**
     * Retrieves a user by username
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<User> getUserByUsername(String username) {
        return userService.findByUsername(username);
    }

    /**
     * Converts a User entity to a UserDTO
     */
    public UserDTO convertToUserDTO(User user) {
        return userMapper.toDto(user);
    }

    /**
     * Converts a UserStory entity to a UserStoryDTO
     */
    public UserStoryDTO convertToUserStoryDTO(UserStory userStory) {
        return userStoryMapper.toDto(userStory);
    }

    /**
     * Verifica se l'utente è il facilitatore della sessione
     */
    public boolean isFacilitator(PlanningPokerSession session, User user) {
        return session.getFacilitator().getId().equals(user.getId());
    }

    /**
     * Verifica se l'utente è un partecipante della sessione
     */
    public boolean isParticipant(PlanningPokerSession session, User user) {
        return session.getParticipants().contains(user);
    }

    /**
     * Verifica se l'utente è il facilitatore o un amministratore
     */
    public boolean hasFacilitatorOrAdminRights(PlanningPokerSession session, User user, UserDetails userDetails) {
        return isFacilitator(session, user) || 
               userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    /**
     * Verifica se l'utente può votare nella sessione
     */
    public boolean canVote(PlanningPokerSession session, User user) {
        return (isParticipant(session, user) || isFacilitator(session, user)) && 
               session.getStatus() == SessionStatus.ACTIVE;
    }

    /**
     * Avvia una sessione dopo aver verificato che l'utente sia il facilitatore
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void startSessionWithAuthorization(Long id, User currentUser) {
        PlanningPokerSession session = getSessionById(id)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        if (!isFacilitator(session, currentUser)) {
            throw new AccessDeniedException("Only the facilitator can start the session");
        }
        
        startSession(id);
    }

    /**
     * Completa una sessione dopo aver verificato che l'utente sia il facilitatore
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void completeSessionWithAuthorization(Long id, User currentUser) {
        PlanningPokerSession session = getSessionById(id)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        if (!isFacilitator(session, currentUser)) {
            throw new AccessDeniedException("Only the facilitator can complete the session");
        }
        
        completeSession(id);
    }

    /**
     * Cancella una sessione dopo aver verificato che l'utente sia il facilitatore
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void cancelSessionWithAuthorization(Long id, User currentUser) {
        PlanningPokerSession session = getSessionById(id)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        if (!isFacilitator(session, currentUser)) {
            throw new AccessDeniedException("Only the facilitator can cancel the session");
        }
        
        cancelSession(id);
    }

    /**
     * Elimina una sessione dopo aver verificato che l'utente sia il facilitatore o un amministratore
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Long deleteSessionWithAuthorization(Long id, User currentUser, UserDetails userDetails) {
        PlanningPokerSession session = getSessionById(id)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        if (!hasFacilitatorOrAdminRights(session, currentUser, userDetails)) {
            throw new AccessDeniedException("Only the facilitator or an admin can delete the session");
        }
        
        Long projectId = session.getProject().getId();
        deleteSession(id);
        return projectId;
    }

    /**
     * Prepara un DTO per il voto dopo aver verificato che l'utente possa votare
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public EstimationVoteDTO prepareVoteDTO(Long sessionId, Long userStoryId, User currentUser) {
        PlanningPokerSession session = getSessionByIdWithParticipants(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        UserStory userStory = getUserStoryById(userStoryId)
                .orElseThrow(() -> new EntityNotFoundException("User story not found"));
        
        if (!canVote(session, currentUser)) {
            throw new AccessDeniedException("User cannot vote in this session");
        }
        
        EstimationVoteDTO voteDTO = new EstimationVoteDTO();
        voteDTO.setSessionId(sessionId);
        voteDTO.setUserStoryId(userStoryId);
        voteDTO.setVoterId(currentUser.getId());
        
        // Check if user has already voted
        findVoteBySessionAndUserStoryAndVoter(session, userStory, currentUser)
                .ifPresent(vote -> {
                    voteDTO.setId(vote.getId());
                    voteDTO.setStoryPoints(vote.getStoryPoints());
                    voteDTO.setReasoning(vote.getReasoning());
                });
        
        return voteDTO;
    }
    
    /**
     * Salva un voto usando il nome utente invece dell'oggetto User
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public EstimationVote saveVoteByUsername(Long sessionId, Long userStoryId, 
                                        String username, Integer storyPoints, String reasoning) {
        User voter = getUserByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        return saveVote(sessionId, userStoryId, voter, storyPoints, reasoning);
    }
}