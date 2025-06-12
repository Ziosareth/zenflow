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
    
    @Transactional(readOnly = true)
    public List<UserStory> findAll() {
        return userStoryRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<UserStory> findById(Long id) {
        return userStoryRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<UserStory> findByProject(Project project) {
        return userStoryRepository.findByProject(project);
    }
    
    @Transactional(readOnly = true)
    public List<UserStory> findByProjectId(Long projectId) {
        return userStoryRepository.findByProjectId(projectId);
    }
    
    @Transactional(readOnly = true)
    public List<UserStory> findByProjectAndStatus(Project project, StoryStatus status) {
        return userStoryRepository.findByProjectAndStatus(project, status);
    }
    
    @Transactional(readOnly = true)
    public List<UserStory> findByProjectIdAndStatus(Long projectId, StoryStatus status) {
        return userStoryRepository.findByProjectIdAndStatus(projectId, status);
    }
    
    @Transactional(readOnly = true)
    public List<UserStory> findByAssignedTo(User user) {
        return userStoryRepository.findByAssignedTo(user);
    }
    
    @Transactional
    public UserStory save(UserStory userStory) {
        // Calculate PERT estimate if all three estimates are provided
        if (userStory.getOptimisticEstimate() != null && 
            userStory.getPessimisticEstimate() != null && 
            userStory.getMostLikelyEstimate() != null) {
            
            double pertEstimate = (userStory.getOptimisticEstimate() + 
                                  (4 * userStory.getMostLikelyEstimate()) + 
                                  userStory.getPessimisticEstimate()) / 6;
            
            userStory.setPertEstimate(pertEstimate);
        }
        
        return userStoryRepository.save(userStory);
    }
    
    @Transactional
    public void deleteById(Long id) {
        userStoryRepository.deleteById(id);
    }
}