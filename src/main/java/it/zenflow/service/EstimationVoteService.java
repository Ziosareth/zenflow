package it.zenflow.service;

import it.zenflow.model.project.*;
import it.zenflow.model.rbac.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstimationVoteService {

    private final EstimationVoteRepository estimationVoteRepository;
    private final PlanningPokerSessionRepository planningPokerSessionRepository;
    private final UserStoryRepository userStoryRepository;

    @Transactional(readOnly = true)
    public List<EstimationVote> findAll() {
        return estimationVoteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<EstimationVote> findById(Long id) {
        return estimationVoteRepository.findById(id);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<EstimationVote> findBySession(PlanningPokerSession session) {
        return estimationVoteRepository.findBySession(session);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<EstimationVote> findBySessionId(Long sessionId) {
        return estimationVoteRepository.findBySessionId(sessionId);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<EstimationVote> findByUserStory(UserStory userStory) {
        return estimationVoteRepository.findByUserStory(userStory);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<EstimationVote> findByUserStoryId(Long userStoryId) {
        return estimationVoteRepository.findByUserStoryId(userStoryId);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public List<EstimationVote> findByVoter(User voter) {
        return estimationVoteRepository.findByVoter(voter);
    }

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public Optional<EstimationVote> findBySessionAndUserStoryAndVoter(
            PlanningPokerSession session, UserStory userStory, User voter) {
        return estimationVoteRepository.findBySessionAndUserStoryAndVoter(session, userStory, voter);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public EstimationVote saveVote(Long sessionId, Long userStoryId, User voter, Integer storyPoints, String reasoning) {
        PlanningPokerSession session = planningPokerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));

        if (session.getStatus() != it.zenflow.model.project.enums.SessionStatus.ACTIVE) {
            throw new IllegalStateException("Cannot vote in a session that is not active");
        }

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new EntityNotFoundException("User story not found"));

        // Check if user has already voted for this story in this session
        Optional<EstimationVote> existingVote = estimationVoteRepository
                .findBySessionAndUserStoryAndVoter(session, userStory, voter);

        EstimationVote vote;
        if (existingVote.isPresent()) {
            // Update existing vote
            vote = existingVote.get();
            vote.setStoryPoints(storyPoints);
            vote.setReasoning(reasoning);
        } else {
            // Create new vote
            vote = new EstimationVote();
            vote.setSession(session);
            vote.setUserStory(userStory);
            vote.setVoter(voter);
            vote.setStoryPoints(storyPoints);
            vote.setReasoning(reasoning);
        }

        return estimationVoteRepository.save(vote);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteVote(Long voteId) {
        estimationVoteRepository.deleteById(voteId);
    }
}