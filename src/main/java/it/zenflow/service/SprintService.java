package it.zenflow.service;

import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.SprintRepository;
import it.zenflow.model.project.enums.SprintStatus;
import it.zenflow.model.project.enums.StoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SprintService {
    private final SprintRepository sprintRepository;
    private final UserStoryService userStoryService;
    private final SprintMetricsService sprintMetricsService;
    
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<Sprint> findByProject(Project project) {
        return sprintRepository.findByProject(project);
    }
    
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Sprint> findById(Long id) {
        return sprintRepository.findById(id);
    }
    
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Sprint> findByIdWithStories(Long id) {
        return sprintRepository.findByIdWithStories(id);
    }
    
    @Transactional(transactionManager = "tenantTransactionManager")
    public Sprint save(Sprint sprint) {
        return sprintRepository.save(sprint);
    }
    
    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteById(Long id) {
        sprintRepository.deleteById(id);
    }
    
    @Transactional(transactionManager = "tenantTransactionManager")
    public void startSprint(Long sprintId) {
        findById(sprintId).ifPresent(sprint -> {
            sprint.setStatus(SprintStatus.ACTIVE);
            sprintRepository.save(sprint);
        });
    }
    
    @Transactional(transactionManager = "tenantTransactionManager")
    public void completeSprint(Long sprintId) {
        findByIdWithStories(sprintId).ifPresent(sprint -> {
            // Aggiorna lo stato delle user story non completate
            sprint.getStories().stream()
                .filter(story -> story.getStatus() != StoryStatus.DONE)
                .forEach(story -> {
                    story.setStatus(StoryStatus.BACKLOG);
                    story.setSprint(null);
                    userStoryService.save(story);
                });
            
            // Calcola i punti completati e la velocità usando SprintMetricsService
            int completedPoints = sprintMetricsService.calculateCompletedStoryPoints(sprint);
            double velocity = sprintMetricsService.calculateSprintVelocity(sprint, completedPoints);
            
            sprint.setCompletedStoryPoints(completedPoints);
            sprint.setSprintVelocity(velocity);
            sprint.setStatus(SprintStatus.COMPLETED);
            sprintRepository.save(sprint);
            
            // Aggiorna la velocità del team nel progetto
            sprintMetricsService.updateProjectVelocity(sprint.getProject().getId());
        });
    }
    
    @Transactional(transactionManager = "tenantTransactionManager")
    public void cancelSprint(Long sprintId) {
        findByIdWithStories(sprintId).ifPresent(sprint -> {
            // Rimuovi tutte le user story dallo sprint
            sprint.getStories().forEach(story -> {
                story.setStatus(StoryStatus.BACKLOG);
                story.setSprint(null);
                userStoryService.save(story);
            });
            
            sprint.setStatus(SprintStatus.CANCELLED);
            sprintRepository.save(sprint);
        });
    }
    
    @Transactional(transactionManager = "tenantTransactionManager")
    public void addUserStoryToSprint(Long sprintId, Long userStoryId) {
        findById(sprintId).ifPresent(sprint -> {
            userStoryService.findById(userStoryId).ifPresent(userStory -> {
                boolean wasDone = userStory.getStatus() == StoryStatus.DONE;
                userStory.setSprint(sprint);
                userStoryService.save(userStory);
                
                // Aggiorna i punti pianificati
                updateSprintPlannedPoints(sprintId);
                
                // Se la story è DONE, aggiorna anche i punti completati/velocity
                if (wasDone) {
                    sprintMetricsService.updateSprintCompletedPoints(sprintId);
                }
            });
        });
    }
    
    @Transactional(transactionManager = "tenantTransactionManager")
    public void removeUserStoryFromSprint(Long sprintId, Long userStoryId) {
        userStoryService.findById(userStoryId).ifPresent(userStory -> {
            if (userStory.getSprint() != null && userStory.getSprint().getId().equals(sprintId)) {
                boolean wasDone = userStory.getStatus() == StoryStatus.DONE;
                userStory.setSprint(null);
                userStoryService.save(userStory);
                
                // Aggiorna i punti pianificati
                updateSprintPlannedPoints(sprintId);
                
                // Se la story era DONE e viene rimossa, aggiorna i punti completati/velocity
                if (wasDone) {
                    sprintMetricsService.updateSprintCompletedPoints(sprintId);
                }
            }
        });
    }
    
    @Transactional(transactionManager = "tenantTransactionManager")
    public void updateSprintPlannedPoints(Long sprintId) {
        sprintMetricsService.updateSprintPlannedPoints(sprintId);
    }
    
    
    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<Sprint> findActiveSprintByProject(Long projectId) {
        return sprintRepository.findByProjectIdOrderByStartDateDesc(projectId).stream()
            .filter(sprint -> sprint.getStatus() == SprintStatus.ACTIVE)
            .findFirst();
    }
}