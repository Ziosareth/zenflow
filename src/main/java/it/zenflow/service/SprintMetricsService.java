package it.zenflow.service;

import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.SprintRepository;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.StoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for calculating and updating sprint metrics.
 * This service centralizes all sprint metrics calculations to avoid circular dependencies
 * between UserStoryService and SprintService.
 */
@Service
@RequiredArgsConstructor
public class SprintMetricsService {
    
    private final SprintRepository sprintRepository;
    private final ProjectService projectService;
    
    /**
     * Updates the planned story points for a sprint based on the story points of all user stories
     * associated with the sprint.
     *
     * @param sprintId the ID of the sprint to update
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void updateSprintPlannedPoints(Long sprintId) {
        findSprintWithStories(sprintId).ifPresent(sprint -> {
            int plannedPoints = sprint.getStories().stream()
                .filter(story -> story.getStoryPoints() != null)
                .mapToInt(UserStory::getStoryPoints)
                .sum();
            
            sprint.setPlannedStoryPoints(plannedPoints);
            sprintRepository.save(sprint);
        });
    }
    
    /**
     * Calculates the completed story points for a sprint based on the story points of all user stories
     * with status DONE associated with the sprint.
     *
     * @param sprint the sprint to calculate completed points for
     * @return the calculated completed story points
     */
    public int calculateCompletedStoryPoints(Sprint sprint) {
        return sprint.getStories().stream()
            .filter(story -> story.getStatus() == StoryStatus.DONE && story.getStoryPoints() != null)
            .mapToInt(UserStory::getStoryPoints)
            .sum();
    }
    
    /**
     * Calculates the sprint velocity based on completed story points and sprint duration.
     *
     * @param sprint the sprint to calculate velocity for
     * @param completedPoints the completed story points
     * @return the calculated sprint velocity
     */
    public double calculateSprintVelocity(Sprint sprint, int completedPoints) {
        // Calculate duration as the difference in days between start and end (exclusive),
        // so a 2-week sprint has 14 days.
        long durationInDays = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate());
        double durationInWeeks = durationInDays / 7.0;
        return durationInWeeks > 0 ? completedPoints / durationInWeeks : 0;
    }
    
    /**
     * Updates the completed story points and velocity of a sprint based on the story points
     * of all user stories with status DONE associated with the sprint.
     *
     * @param sprintId the ID of the sprint to update
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void updateSprintCompletedPoints(Long sprintId) {
        findSprintWithStories(sprintId).ifPresent(sprint -> {
            int completedPoints = calculateCompletedStoryPoints(sprint);
            double velocity = calculateSprintVelocity(sprint, completedPoints);
            
            sprint.setCompletedStoryPoints(completedPoints);
            sprint.setSprintVelocity(velocity);
            sprintRepository.save(sprint);
            
            // Update the project velocity as well
            updateProjectVelocity(sprint.getProject().getId());
            
            // Also update project's completed story points to keep project metrics in sync
            projectService.updateProjectCompletedStoryPoints(sprint.getProject().getId());
        });
    }
    
    /**
     * Updates the project velocity based on the average velocity of completed sprints.
     *
     * @param projectId the ID of the project to update
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void updateProjectVelocity(Long projectId) {
        // Find all completed sprints for the project
        List<Sprint> completedSprints = sprintRepository.findCompletedSprintsByProjectId(projectId);

        // If there are no completed sprints, do not update the project's velocity
        if (completedSprints == null || completedSprints.isEmpty()) {
            return;
        }

        // Calculate the average velocity of completed sprints (filtering out nulls)
        double averageVelocity = completedSprints.stream()
            .map(Sprint::getSprintVelocity)
            .filter(java.util.Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        // Update the project's velocity
        projectService.updateProjectVelocity(projectId, averageVelocity);
    }
    
    /**
     * Helper method to find a sprint with its stories.
     *
     * @param sprintId the ID of the sprint to find
     * @return an Optional containing the sprint with stories, or empty if not found
     */
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    protected Optional<Sprint> findSprintWithStories(Long sprintId) {
        return sprintRepository.findByIdWithStories(sprintId);
    }
}