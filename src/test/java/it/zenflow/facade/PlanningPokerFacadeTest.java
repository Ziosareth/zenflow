package it.zenflow.facade;

import it.zenflow.dto.CreatePlanningPokerSessionCommand;
import it.zenflow.dto.EstimationVoteDTO;
import it.zenflow.dto.UserDTO;
import it.zenflow.dto.UserStoryDTO;
import it.zenflow.model.project.EstimationVote;
import it.zenflow.model.project.PlanningPokerSession;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.SessionStatus;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.EstimationVoteService;
import it.zenflow.service.PlanningPokerSessionService;
import it.zenflow.service.ProjectService;
import it.zenflow.service.UserStoryService;
import it.zenflow.service.rbac.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlanningPokerFacadeTest {

    @Mock
    private PlanningPokerSessionService planningPokerSessionService;

    @Mock
    private EstimationVoteService estimationVoteService;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserStoryService userStoryService;

    @Mock
    private UserService userService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private PlanningPokerFacade planningPokerFacade;

    // Test data
    private PlanningPokerSession session1;
    private PlanningPokerSession session2;
    private User facilitatorUser;
    private User participantUser;
    private User regularUser;
    private UserDetails facilitatorUserDetails;
    private UserDetails adminUserDetails;
    private Project project;
    private UserStory userStory;
    private EstimationVote vote;
    private CreatePlanningPokerSessionCommand command;
    private Set<User> participants;

    @BeforeEach
    public void setup() {
        // Create test users
        facilitatorUser = new User();
        facilitatorUser.setId(1L);
        facilitatorUser.setUsername("facilitator");
        facilitatorUser.setEmail("facilitator@example.com");

        participantUser = new User();
        participantUser.setId(2L);
        participantUser.setUsername("participant");
        participantUser.setEmail("participant@example.com");

        regularUser = new User();
        regularUser.setId(3L);
        regularUser.setUsername("regular");
        regularUser.setEmail("regular@example.com");

        // Create participants set
        participants = new HashSet<>();
        participants.add(participantUser);

        // Create test project
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        project.setDescription("This is a test project");

        // Create test user story
        userStory = new UserStory();
        userStory.setId(1L);
        userStory.setTitle("Test User Story");
        userStory.setDescription("This is a test user story");
        userStory.setProject(project);

        // Create test sessions
        session1 = new PlanningPokerSession();
        session1.setId(1L);
        session1.setName("Test Session 1");
        session1.setStatus(SessionStatus.CREATED);
        session1.setFacilitator(facilitatorUser);
        session1.setParticipants(participants);
        session1.setProject(project);
        session1.setUserStory(userStory);

        session2 = new PlanningPokerSession();
        session2.setId(2L);
        session2.setName("Test Session 2");
        session2.setStatus(SessionStatus.ACTIVE);
        session2.setFacilitator(facilitatorUser);
        session2.setParticipants(new HashSet<>());
        session2.setProject(project);

        // Create test vote
        vote = new EstimationVote();
        vote.setId(1L);
        vote.setSession(session1);
        vote.setUserStory(userStory);
        vote.setVoter(participantUser);
        vote.setStoryPoints(5);
        vote.setReasoning("This is a reasonable estimate");

        // Create test command
        command = new CreatePlanningPokerSessionCommand();
        command.setName("New Session");
        command.setProjectId(project.getId());
        command.setUserStoryId(userStory.getId());

        // Create UserDetails for authentication tests
        facilitatorUserDetails = createUserDetails(facilitatorUser.getUsername(), Collections.singletonList("USER"));
        adminUserDetails = createUserDetails("admin", Arrays.asList("USER", "ADMIN"));
    }

    private UserDetails createUserDetails(String username, List<String> roles) {
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            @Override
            public String getPassword() {
                return "password";
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }

    @Test
    public void testGetAllSessions() {
        // Arrange
        List<PlanningPokerSession> sessions = Arrays.asList(session1, session2);
        when(planningPokerSessionService.findAll()).thenReturn(sessions);

        // Act
        List<PlanningPokerSession> result = planningPokerFacade.getAllSessions();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(session1, session2);
        verify(planningPokerSessionService, times(1)).findAll();
    }

    @Test
    public void testGetSessionsByProjectId() {
        // Arrange
        List<PlanningPokerSession> sessions = Arrays.asList(session1, session2);
        when(planningPokerSessionService.findByProjectId(project.getId())).thenReturn(sessions);

        // Act
        List<PlanningPokerSession> result = planningPokerFacade.getSessionsByProjectId(project.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(session1, session2);
        verify(planningPokerSessionService, times(1)).findByProjectId(project.getId());
    }

    @Test
    public void testGetSessionById() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));

        // Act
        Optional<PlanningPokerSession> result = planningPokerFacade.getSessionById(session1.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(session1);
        verify(planningPokerSessionService, times(1)).findById(session1.getId());
    }

    @Test
    public void testGetSessionByIdWithParticipants() {
        // Arrange
        when(planningPokerSessionService.findByIdWithParticipants(session1.getId())).thenReturn(Optional.of(session1));

        // Act
        Optional<PlanningPokerSession> result = planningPokerFacade.getSessionByIdWithParticipants(session1.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(session1);
        verify(planningPokerSessionService, times(1)).findByIdWithParticipants(session1.getId());
    }

    @Test
    public void testGetSessionByIdWithParticipantsAndVotes() {
        // Arrange
        when(planningPokerSessionService.findByIdWithParticipantsAndVotes(session1.getId())).thenReturn(Optional.of(session1));

        // Act
        Optional<PlanningPokerSession> result = planningPokerFacade.getSessionByIdWithParticipantsAndVotes(session1.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(session1);
        verify(planningPokerSessionService, times(1)).findByIdWithParticipantsAndVotes(session1.getId());
    }

    @Test
    public void testCreateSession() {
        // Arrange
        when(planningPokerSessionService.createSession(command, facilitatorUser)).thenReturn(session1);

        // Act
        PlanningPokerSession result = planningPokerFacade.createSession(command, facilitatorUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(session1);
        verify(planningPokerSessionService, times(1)).createSession(command, facilitatorUser);
    }

    @Test
    public void testStartSession() {
        // Arrange
        when(planningPokerSessionService.startSession(session1.getId())).thenReturn(session1);

        // Act
        planningPokerFacade.startSession(session1.getId());

        // Assert
        verify(planningPokerSessionService, times(1)).startSession(session1.getId());
    }

    @Test
    public void testCompleteSession() {
        // Arrange
        when(planningPokerSessionService.completeSession(session1.getId())).thenReturn(session1);

        // Act
        planningPokerFacade.completeSession(session1.getId());

        // Assert
        verify(planningPokerSessionService, times(1)).completeSession(session1.getId());
    }

    @Test
    public void testCancelSession() {
        // Arrange
        when(planningPokerSessionService.cancelSession(session1.getId())).thenReturn(session1);

        // Act
        planningPokerFacade.cancelSession(session1.getId());

        // Assert
        verify(planningPokerSessionService, times(1)).cancelSession(session1.getId());
    }

    @Test
    public void testDeleteSession() {
        // Arrange
        doNothing().when(planningPokerSessionService).deleteSession(session1.getId());

        // Act
        planningPokerFacade.deleteSession(session1.getId());

        // Assert
        verify(planningPokerSessionService, times(1)).deleteSession(session1.getId());
    }

    @Test
    public void testGetVotesBySessionId() {
        // Arrange
        List<EstimationVote> votes = Collections.singletonList(vote);
        when(estimationVoteService.findBySessionIdWithVoter(session1.getId())).thenReturn(votes);

        // Act
        List<EstimationVote> result = planningPokerFacade.getVotesBySessionId(session1.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(vote);
        verify(estimationVoteService, times(1)).findBySessionIdWithVoter(session1.getId());
    }

    @Test
    public void testFindVoteBySessionAndUserStoryAndVoter() {
        // Arrange
        when(estimationVoteService.findBySessionAndUserStoryAndVoter(session1, userStory, participantUser))
                .thenReturn(Optional.of(vote));

        // Act
        Optional<EstimationVote> result = planningPokerFacade.findVoteBySessionAndUserStoryAndVoter(
                session1, userStory, participantUser);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(vote);
        verify(estimationVoteService, times(1))
                .findBySessionAndUserStoryAndVoter(session1, userStory, participantUser);
    }

    @Test
    public void testSaveVote() {
        // Arrange
        when(estimationVoteService.saveVote(session1.getId(), userStory.getId(), participantUser, 5, "Reasoning"))
                .thenReturn(vote);

        // Act
        EstimationVote result = planningPokerFacade.saveVote(
                session1.getId(), userStory.getId(), participantUser, 5, "Reasoning");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(vote);
        verify(estimationVoteService, times(1))
                .saveVote(session1.getId(), userStory.getId(), participantUser, 5, "Reasoning");
    }

    @Test
    public void testSaveVoteByUsername() {
        // Arrange
        when(userService.findByUsername(participantUser.getUsername())).thenReturn(Optional.of(participantUser));
        when(estimationVoteService.saveVote(session1.getId(), userStory.getId(), participantUser, 5, "Reasoning"))
                .thenReturn(vote);

        // Act
        EstimationVote result = planningPokerFacade.saveVoteByUsername(
                session1.getId(), userStory.getId(), participantUser.getUsername(), 5, "Reasoning");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(vote);
        verify(userService, times(1)).findByUsername(participantUser.getUsername());
        verify(estimationVoteService, times(1))
                .saveVote(session1.getId(), userStory.getId(), participantUser, 5, "Reasoning");
    }

    @Test
    public void testSaveVoteByUsername_UserNotFound() {
        // Arrange
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planningPokerFacade.saveVoteByUsername(
                session1.getId(), userStory.getId(), "nonexistent", 5, "Reasoning"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
        
        verify(userService, times(1)).findByUsername("nonexistent");
        verify(estimationVoteService, never())
                .saveVote(anyLong(), anyLong(), any(User.class), anyInt(), anyString());
    }

    @Test
    public void testIsFacilitator_WhenUserIsFacilitator() {
        // Act
        boolean result = planningPokerFacade.isFacilitator(session1, facilitatorUser);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testIsFacilitator_WhenUserIsNotFacilitator() {
        // Act
        boolean result = planningPokerFacade.isFacilitator(session1, participantUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void testIsParticipant_WhenUserIsParticipant() {
        // Act
        boolean result = planningPokerFacade.isParticipant(session1, participantUser);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testIsParticipant_WhenUserIsNotParticipant() {
        // Act
        boolean result = planningPokerFacade.isParticipant(session1, regularUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void testCanVote_WhenUserIsFacilitatorAndSessionIsActive() {
        // Arrange
        session1.setStatus(SessionStatus.ACTIVE);

        // Act
        boolean result = planningPokerFacade.canVote(session1, facilitatorUser);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testCanVote_WhenUserIsParticipantAndSessionIsActive() {
        // Arrange
        session1.setStatus(SessionStatus.ACTIVE);

        // Act
        boolean result = planningPokerFacade.canVote(session1, participantUser);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testCanVote_WhenUserIsNotParticipantOrFacilitator() {
        // Arrange
        session1.setStatus(SessionStatus.ACTIVE);

        // Act
        boolean result = planningPokerFacade.canVote(session1, regularUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void testCanVote_WhenSessionIsNotActive() {
        // Arrange
        session1.setStatus(SessionStatus.CREATED);

        // Act
        boolean result = planningPokerFacade.canVote(session1, participantUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void testHasFacilitatorOrAdminRights_WhenUserIsFacilitator() {
        // Act
        boolean result = planningPokerFacade.hasFacilitatorOrAdminRights(session1, facilitatorUser, facilitatorUserDetails);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testHasFacilitatorOrAdminRights_WhenUserIsAdmin() {
        // Act
        boolean result = planningPokerFacade.hasFacilitatorOrAdminRights(session1, regularUser, adminUserDetails);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    public void testHasFacilitatorOrAdminRights_WhenUserIsNotFacilitatorOrAdmin() {
        // Act
        boolean result = planningPokerFacade.hasFacilitatorOrAdminRights(session1, regularUser, facilitatorUserDetails);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    public void testStartSessionWithAuthorization_Success() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));
        when(planningPokerSessionService.startSession(session1.getId())).thenReturn(session1);

        // Act
        planningPokerFacade.startSessionWithAuthorization(session1.getId(), facilitatorUser);

        // Assert
        verify(planningPokerSessionService, times(1)).findById(session1.getId());
        verify(planningPokerSessionService, times(1)).startSession(session1.getId());
    }

    @Test
    public void testStartSessionWithAuthorization_NotFacilitator() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));

        // Act & Assert
        assertThatThrownBy(() -> planningPokerFacade.startSessionWithAuthorization(session1.getId(), regularUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only the facilitator can start the session");

        verify(planningPokerSessionService, times(1)).findById(session1.getId());
        verify(planningPokerSessionService, never()).startSession(anyLong());
    }

    @Test
    public void testStartSessionWithAuthorization_SessionNotFound() {
        // Arrange
        when(planningPokerSessionService.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planningPokerFacade.startSessionWithAuthorization(999L, facilitatorUser))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Session not found");

        verify(planningPokerSessionService, times(1)).findById(999L);
        verify(planningPokerSessionService, never()).startSession(anyLong());
    }

    @Test
    public void testCompleteSessionWithAuthorization_Success() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));
        when(planningPokerSessionService.completeSession(session1.getId())).thenReturn(session1);

        // Act
        planningPokerFacade.completeSessionWithAuthorization(session1.getId(), facilitatorUser);

        // Assert
        verify(planningPokerSessionService, times(1)).findById(session1.getId());
        verify(planningPokerSessionService, times(1)).completeSession(session1.getId());
    }

    @Test
    public void testCompleteSessionWithAuthorization_NotFacilitator() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));

        // Act & Assert
        assertThatThrownBy(() -> planningPokerFacade.completeSessionWithAuthorization(session1.getId(), regularUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only the facilitator can complete the session");

        verify(planningPokerSessionService, times(1)).findById(session1.getId());
        verify(planningPokerSessionService, never()).completeSession(anyLong());
    }

    @Test
    public void testCancelSessionWithAuthorization_Success() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));
        when(planningPokerSessionService.cancelSession(session1.getId())).thenReturn(session1);

        // Act
        planningPokerFacade.cancelSessionWithAuthorization(session1.getId(), facilitatorUser);

        // Assert
        verify(planningPokerSessionService, times(1)).findById(session1.getId());
        verify(planningPokerSessionService, times(1)).cancelSession(session1.getId());
    }

    @Test
    public void testCancelSessionWithAuthorization_NotFacilitator() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));

        // Act & Assert
        assertThatThrownBy(() -> planningPokerFacade.cancelSessionWithAuthorization(session1.getId(), regularUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only the facilitator can cancel the session");

        verify(planningPokerSessionService, times(1)).findById(session1.getId());
        verify(planningPokerSessionService, never()).cancelSession(anyLong());
    }

    @Test
    public void testDeleteSessionWithAuthorization_AsFacilitator() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));
        doNothing().when(planningPokerSessionService).deleteSession(session1.getId());

        // Act
        Long result = planningPokerFacade.deleteSessionWithAuthorization(session1.getId(), facilitatorUser, facilitatorUserDetails);

        // Assert
        assertThat(result).isEqualTo(project.getId());
        verify(planningPokerSessionService, times(1)).findById(session1.getId());
        verify(planningPokerSessionService, times(1)).deleteSession(session1.getId());
    }

    @Test
    public void testDeleteSessionWithAuthorization_AsAdmin() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));
        doNothing().when(planningPokerSessionService).deleteSession(session1.getId());

        // Act
        Long result = planningPokerFacade.deleteSessionWithAuthorization(session1.getId(), regularUser, adminUserDetails);

        // Assert
        assertThat(result).isEqualTo(project.getId());
        verify(planningPokerSessionService, times(1)).findById(session1.getId());
        verify(planningPokerSessionService, times(1)).deleteSession(session1.getId());
    }

    @Test
    public void testDeleteSessionWithAuthorization_NotAuthorized() {
        // Arrange
        when(planningPokerSessionService.findById(session1.getId())).thenReturn(Optional.of(session1));

        // Act & Assert
        assertThatThrownBy(() -> planningPokerFacade.deleteSessionWithAuthorization(session1.getId(), regularUser, facilitatorUserDetails))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only the facilitator or an admin can delete the session");

        verify(planningPokerSessionService, times(1)).findById(session1.getId());
        verify(planningPokerSessionService, never()).deleteSession(anyLong());
    }

    @Test
    public void testConvertToUserDTO() {
        // Arrange
        User user = facilitatorUser;
        user.setTenant("test-tenant");
        user.setEnabled(true);

        // Act
        UserDTO result = planningPokerFacade.convertToUserDTO(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.isEnabled()).isEqualTo(user.isEnabled());
        assertThat(result.getTenant()).isEqualTo(user.getTenant());
    }

    @Test
    public void testConvertToUserStoryDTO() {
        // Arrange
        userStory.setStatus(StoryStatus.BACKLOG);
        userStory.setPriority(Priority.MEDIUM);
        userStory.setStoryPoints(5);
        userStory.setEstimationType(null); // Assuming this can be null

        // Act
        UserStoryDTO result = planningPokerFacade.convertToUserStoryDTO(userStory);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userStory.getId());
        assertThat(result.getTitle()).isEqualTo(userStory.getTitle());
        assertThat(result.getDescription()).isEqualTo(userStory.getDescription());
        assertThat(result.getStatus()).isEqualTo(userStory.getStatus());
        assertThat(result.getPriority()).isEqualTo(userStory.getPriority());
        assertThat(result.getStoryPoints()).isEqualTo(userStory.getStoryPoints());
        assertThat(result.getProjectId()).isEqualTo(userStory.getProject().getId());
        assertThat(result.getEstimationType()).isEqualTo(userStory.getEstimationType());
    }

    @Test
    public void testPrepareVoteDTO() {
        // Arrange
        session1.setStatus(SessionStatus.ACTIVE);
        when(planningPokerSessionService.findByIdWithParticipants(session1.getId())).thenReturn(Optional.of(session1));
        when(userStoryService.findById(userStory.getId())).thenReturn(Optional.of(userStory));
        when(estimationVoteService.findBySessionAndUserStoryAndVoter(session1, userStory, participantUser))
                .thenReturn(Optional.of(vote));

        // Act
        EstimationVoteDTO result = planningPokerFacade.prepareVoteDTO(session1.getId(), userStory.getId(), participantUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(session1.getId());
        assertThat(result.getUserStoryId()).isEqualTo(userStory.getId());
        assertThat(result.getVoterId()).isEqualTo(participantUser.getId());
        assertThat(result.getId()).isEqualTo(vote.getId());
        assertThat(result.getStoryPoints()).isEqualTo(vote.getStoryPoints());
        assertThat(result.getReasoning()).isEqualTo(vote.getReasoning());
        
        verify(planningPokerSessionService, times(1)).findByIdWithParticipants(session1.getId());
        verify(userStoryService, times(1)).findById(userStory.getId());
        verify(estimationVoteService, times(1)).findBySessionAndUserStoryAndVoter(session1, userStory, participantUser);
    }

    @Test
    public void testPrepareVoteDTO_NoExistingVote() {
        // Arrange
        session1.setStatus(SessionStatus.ACTIVE);
        when(planningPokerSessionService.findByIdWithParticipants(session1.getId())).thenReturn(Optional.of(session1));
        when(userStoryService.findById(userStory.getId())).thenReturn(Optional.of(userStory));
        when(estimationVoteService.findBySessionAndUserStoryAndVoter(session1, userStory, participantUser))
                .thenReturn(Optional.empty());

        // Act
        EstimationVoteDTO result = planningPokerFacade.prepareVoteDTO(session1.getId(), userStory.getId(), participantUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(session1.getId());
        assertThat(result.getUserStoryId()).isEqualTo(userStory.getId());
        assertThat(result.getVoterId()).isEqualTo(participantUser.getId());
        assertThat(result.getId()).isNull();
        assertThat(result.getStoryPoints()).isNull();
        assertThat(result.getReasoning()).isNull();
        
        verify(planningPokerSessionService, times(1)).findByIdWithParticipants(session1.getId());
        verify(userStoryService, times(1)).findById(userStory.getId());
        verify(estimationVoteService, times(1)).findBySessionAndUserStoryAndVoter(session1, userStory, participantUser);
    }

    @Test
    public void testPrepareVoteDTO_UserCannotVote() {
        // Arrange
        session1.setStatus(SessionStatus.CREATED); // Not active
        when(planningPokerSessionService.findByIdWithParticipants(session1.getId())).thenReturn(Optional.of(session1));
        when(userStoryService.findById(userStory.getId())).thenReturn(Optional.of(userStory));

        // Act & Assert
        assertThatThrownBy(() -> planningPokerFacade.prepareVoteDTO(session1.getId(), userStory.getId(), participantUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User cannot vote in this session");
        
        verify(planningPokerSessionService, times(1)).findByIdWithParticipants(session1.getId());
        verify(userStoryService, times(1)).findById(userStory.getId());
        verify(estimationVoteService, never()).findBySessionAndUserStoryAndVoter(any(), any(), any());
    }

}