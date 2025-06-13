package it.zenflow.model.project;

import it.zenflow.model.rbac.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstimationVoteRepository extends JpaRepository<EstimationVote, Long> {
    
    List<EstimationVote> findBySession(PlanningPokerSession session);
    
    List<EstimationVote> findBySessionId(Long sessionId);
    
    List<EstimationVote> findByUserStory(UserStory userStory);
    
    List<EstimationVote> findByUserStoryId(Long userStoryId);
    
    List<EstimationVote> findByVoter(User voter);
    
    Optional<EstimationVote> findBySessionAndUserStoryAndVoter(
        PlanningPokerSession session, UserStory userStory, User voter);
}