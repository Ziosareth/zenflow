package it.zenflow.service;

import it.zenflow.model.project.Project;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.UserStoryRepository;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserStoryService {

    private final UserStoryRepository userStoryRepository;
    private final ProjectService projectService;
    private final SprintMetricsService sprintMetricsService;

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> findAll() {
        return userStoryRepository.findAll();
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<UserStory> findById(Long id) {
        return userStoryRepository.findById(id);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<UserStory> findByIdWithTasks(Long id) {
        return userStoryRepository.findByIdWithTasks(id);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> findByProject(Project project) {
        return userStoryRepository.findByProjectWithAssignedUser(project);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> findByProjectId(Long projectId) {
        return userStoryRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> findByProjectAndStatus(Project project, StoryStatus status) {
        return userStoryRepository.findByProjectAndStatus(project, status);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> findByProjectIdAndStatus(Long projectId, StoryStatus status) {
        return userStoryRepository.findByProjectIdAndStatus(projectId, status);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> findByAssignedTo(User user) {
        return userStoryRepository.findByAssignedTo(user);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<UserStory> findUnassignedUserStories(Project project) {
        return userStoryRepository.findByProjectAndSprintIsNull(project);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public UserStory save(UserStory userStory) {
        // Handle estimation based on the selected estimation type
        if (userStory.getEstimationType() == null) {
            // Default to STORY_POINTS if not specified
            userStory.setEstimationType(it.zenflow.model.project.enums.EstimationType.STORY_POINTS);
        }

        switch (userStory.getEstimationType()) {
            case PERT:
                // Calculate PERT estimate if all three estimates are provided
                if (userStory.getOptimisticEstimate() != null && 
                    userStory.getPessimisticEstimate() != null && 
                    userStory.getMostLikelyEstimate() != null) {

                    double pertEstimate = (userStory.getOptimisticEstimate() + 
                                          (4 * userStory.getMostLikelyEstimate()) + 
                                          userStory.getPessimisticEstimate()) / 6;

                    userStory.setPertEstimate(pertEstimate);

                    // Calculate variance: ((Pessimistic - Optimistic) / 6)²
                    double variance = Math.pow((userStory.getPessimisticEstimate() - userStory.getOptimisticEstimate()) / 6, 2);
                    userStory.setVariance(variance);

                    // Calculate story points from PERT estimate (rounded to nearest integer)
                    userStory.setStoryPoints((int) Math.round(pertEstimate));
                } else {
                    // Clear story points if PERT estimates are incomplete
                    userStory.setStoryPoints(null);
                    userStory.setPertEstimate(null);
                    userStory.setVariance(null);
                }
                break;

            case STORY_POINTS:
                // Clear all PERT-related fields
                userStory.setOptimisticEstimate(null);
                userStory.setPessimisticEstimate(null);
                userStory.setMostLikelyEstimate(null);
                userStory.setPertEstimate(null);
                userStory.setVariance(null);
                break;
        }

        // Save the user story
        UserStory savedUserStory = userStoryRepository.save(userStory);

        // Update the project's total story points in a separate transaction
        if (userStory.getProject() != null) {
            Long projectId = userStory.getProject().getId();
            projectService.updateProjectTotalStoryPoints(projectId);
        }
        
        // Update the sprint's planned story points if the user story is associated with a sprint
        if (userStory.getSprint() != null) {
            Long sprintId = userStory.getSprint().getId();
            sprintMetricsService.updateSprintPlannedPoints(sprintId);
        }

        return savedUserStory;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteById(Long id) {
        // Get the user story and its project before deleting
        Optional<UserStory> userStoryOpt = userStoryRepository.findByIdWithProject(id);
        Long projectId = null;

        if (userStoryOpt.isPresent() && userStoryOpt.get().getProject() != null) {
            projectId = userStoryOpt.get().getProject().getId();
        }

        // Delete the user story
        userStoryRepository.deleteById(id);

        // Update the project's total story points in a separate transaction
        if (projectId != null) {
            projectService.updateProjectTotalStoryPoints(projectId);
        }
    }
}
