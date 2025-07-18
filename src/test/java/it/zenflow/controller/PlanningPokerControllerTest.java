package it.zenflow.controller;

import it.zenflow.dto.CreatePlanningPokerSessionCommand;
import it.zenflow.model.project.PlanningPokerSession;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.EstimationType;
import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.EstimationVoteService;
import it.zenflow.service.PlanningPokerSessionService;
import it.zenflow.service.ProjectService;
import it.zenflow.service.UserStoryService;
import it.zenflow.service.rbac.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PlanningPokerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserStoryService userStoryService;

    @Autowired
    private PlanningPokerSessionService planningPokerSessionService;

    @Autowired
    private EstimationVoteService estimationVoteService;

    private Project testProject;
    private User testUser;
    private UserStory testUserStory;

    @BeforeEach
    public void setup() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setEnabled(true);
        userService.save(testUser);

        // Create test project
        testProject = new Project();
        testProject.setName("Test Project");
        testProject.setDescription("Test Project Description");
        testProject.setStatus(ProjectStatus.ACTIVE);
        testProject.setType(ProjectType.SCRUM);
        testProject.setOwner(testUser);
        testProject = projectService.save(testProject);

        // Create test user story
        testUserStory = new UserStory();
        testUserStory.setTitle("Test User Story");
        testUserStory.setDescription("Test User Story Description");
        testUserStory.setStatus(StoryStatus.BACKLOG);
        testUserStory.setProject(testProject);
        testUserStory = userStoryService.save(testUserStory);
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"READ_PLANNING_POKER_SESSION", "TENANT_test"})
    public void testListSessions() throws Exception {
        mockMvc.perform(get("/planning-poker"))
                .andExpect(status().isOk())
                .andExpect(view().name("planning-poker/list"))
                .andExpect(model().attributeExists("sessions"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"CREATE_PLANNING_POKER_SESSION", "TENANT_test"})
    public void testCreateSession() throws Exception {
        mockMvc.perform(post("/planning-poker/new")
                        .param("name", "Test Planning Poker Session")
                        .param("projectId", testProject.getId().toString())
                        .param("userStoryId", testUserStory.getId().toString())
                        .param("participantIds", testUser.getId().toString())
                        .param("estimationType", EstimationType.STORY_POINTS.toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/planning-poker/*"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"READ_PLANNING_POKER_SESSION", "CREATE_PLANNING_POKER_SESSION", "TENANT_test"})
    public void testViewSession() throws Exception {
        // Create a session first
        CreatePlanningPokerSessionCommand command = new CreatePlanningPokerSessionCommand();
        command.setName("Test Session");
        command.setProjectId(testProject.getId());
        command.setParticipantIds(Collections.singletonList(testUser.getId()));
        command.setUserStoryId(testUserStory.getId());
        command.setEstimationType(EstimationType.STORY_POINTS);

        PlanningPokerSession savedSession = planningPokerSessionService.createSession(command, testUser);

        mockMvc.perform(get("/planning-poker/{id}", savedSession.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("planning-poker/view"))
                .andExpect(model().attributeExists("pokerSession"))
                .andExpect(content().string(containsString("Test Session")));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"UPDATE_PLANNING_POKER_SESSION", "CREATE_PLANNING_POKER_SESSION", "TENANT_test"})
    public void testStartSession() throws Exception {
        // Create a session first
        CreatePlanningPokerSessionCommand command = new CreatePlanningPokerSessionCommand();
        command.setName("Test Session");
        command.setProjectId(testProject.getId());
        command.setParticipantIds(Collections.singletonList(testUser.getId()));
        command.setUserStoryId(testUserStory.getId());
        command.setEstimationType(EstimationType.STORY_POINTS);

        PlanningPokerSession savedSession = planningPokerSessionService.createSession(command, testUser);

        mockMvc.perform(post("/planning-poker/{id}/start", savedSession.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planning-poker/" + savedSession.getId()));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"READ_PLANNING_POKER_SESSION", "CREATE_ESTIMATION_VOTE", "TENANT_test"})
    public void testVoteForm() throws Exception {
        // Create a session first
        CreatePlanningPokerSessionCommand command = new CreatePlanningPokerSessionCommand();
        command.setName("Test Session");
        command.setProjectId(testProject.getId());
        command.setParticipantIds(Collections.singletonList(testUser.getId()));
        command.setUserStoryId(testUserStory.getId());
        command.setEstimationType(EstimationType.STORY_POINTS);

        PlanningPokerSession createdSession = planningPokerSessionService.createSession(command, testUser);

        // Start the session
        PlanningPokerSession savedSession = planningPokerSessionService.startSession(createdSession.getId());

        mockMvc.perform(get("/planning-poker/{id}/vote/{userStoryId}", 
                        savedSession.getId(), testUserStory.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("planning-poker/vote"))
                .andExpect(model().attributeExists("pokerSession", "userStory", "vote"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"UPDATE_PLANNING_POKER_SESSION", "CREATE_PLANNING_POKER_SESSION", "TENANT_test"})
    public void testCompleteSession() throws Exception {
        // Create a session first
        CreatePlanningPokerSessionCommand command = new CreatePlanningPokerSessionCommand();
        command.setName("Test Session");
        command.setProjectId(testProject.getId());
        command.setParticipantIds(Collections.singletonList(testUser.getId()));
        command.setUserStoryId(testUserStory.getId());
        command.setEstimationType(EstimationType.STORY_POINTS);

        PlanningPokerSession createdSession = planningPokerSessionService.createSession(command, testUser);

        // Start the session
        PlanningPokerSession savedSession = planningPokerSessionService.startSession(createdSession.getId());

        mockMvc.perform(post("/planning-poker/{id}/complete", savedSession.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planning-poker/" + savedSession.getId()));
    }
}
