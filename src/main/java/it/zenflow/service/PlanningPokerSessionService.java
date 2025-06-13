package it.zenflow.service;

import it.zenflow.dto.CreatePlanningPokerSessionCommand;
import it.zenflow.model.project.*;
import it.zenflow.model.project.enums.EstimationType;
import it.zenflow.model.project.enums.SessionStatus;
import it.zenflow.model.rbac.User;
import it.zenflow.service.rbac.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanningPokerSessionService {

    private final PlanningPokerSessionRepository planningPokerSessionRepository;
    private final EstimationVoteRepository estimationVoteRepository;
    private final UserStoryRepository userStoryRepository;
    private final UserService userService;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<PlanningPokerSession> findAll() {
        return planningPokerSessionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<PlanningPokerSession> findById(Long id) {
        return planningPokerSessionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<PlanningPokerSession> findByIdWithParticipants(Long id) {
        return planningPokerSessionRepository.findByIdWithParticipants(id);
    }

    @Transactional(readOnly = true)
    public Optional<PlanningPokerSession> findByIdWithVotes(Long id) {
        return planningPokerSessionRepository.findByIdWithVotes(id);
    }

    @Transactional(readOnly = true)
    public Optional<PlanningPokerSession> findByIdWithParticipantsAndVotes(Long id) {
        return planningPokerSessionRepository.findByIdWithParticipantsAndVotes(id);
    }

    @Transactional(readOnly = true)
    public List<PlanningPokerSession> findByProject(Project project) {
        return planningPokerSessionRepository.findByProject(project);
    }

    @Transactional(readOnly = true)
    public List<PlanningPokerSession> findByProjectId(Long projectId) {
        return planningPokerSessionRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<PlanningPokerSession> findByFacilitator(User facilitator) {
        return planningPokerSessionRepository.findByFacilitator(facilitator);
    }

    @Transactional(readOnly = true)
    public List<PlanningPokerSession> findByStatus(SessionStatus status) {
        return planningPokerSessionRepository.findByStatus(status);
    }

    @Transactional
    public PlanningPokerSession createSession(CreatePlanningPokerSessionCommand command, User facilitator) {
        Project project = projectService.findById(command.getProjectId())
            .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        PlanningPokerSession session = new PlanningPokerSession();
        session.setName(command.getName());
        session.setStatus(SessionStatus.CREATED);
        session.setProject(project);
        session.setFacilitator(facilitator);
        session.setEstimationType(command.getEstimationType());

        // Add participants
        Set<User> participants = new HashSet<>();
        for (Long userId : command.getParticipantIds()) {
            User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
            participants.add(user);
        }
        session.setParticipants(participants);

        // Add user story
        UserStory userStory = userStoryRepository.findById(command.getUserStoryId())
            .orElseThrow(() -> new EntityNotFoundException("User story not found: " + command.getUserStoryId()));
        session.setUserStory(userStory);

        // Save the session
        return planningPokerSessionRepository.save(session);
    }

    @Transactional
    public PlanningPokerSession startSession(Long sessionId) {
        PlanningPokerSession session = planningPokerSessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found"));

        if (session.getStatus() != SessionStatus.CREATED) {
            throw new IllegalStateException("Session is not in CREATED state");
        }

        session.setStatus(SessionStatus.ACTIVE);
        return planningPokerSessionRepository.save(session);
    }

    @Transactional
    public PlanningPokerSession completeSession(Long sessionId) {
        PlanningPokerSession session = planningPokerSessionRepository.findByIdWithVotes(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found"));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Session is not in ACTIVE state");
        }

        // Process votes and update user stories
        processVotesAndUpdateUserStories(session);

        session.setStatus(SessionStatus.COMPLETED);
        return planningPokerSessionRepository.save(session);
    }

    @Transactional
    public PlanningPokerSession cancelSession(Long sessionId) {
        PlanningPokerSession session = planningPokerSessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found"));

        session.setStatus(SessionStatus.CANCELLED);
        return planningPokerSessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        planningPokerSessionRepository.deleteById(sessionId);
    }

    private void processVotesAndUpdateUserStories(PlanningPokerSession session) {
        UserStory userStory = session.getUserStory();
        if (userStory == null) {
            return;
        }

        List<EstimationVote> votes = session.getVotes();
        if (votes.isEmpty()) {
            return;
        }

        // Calculate the final estimate based on votes
        if (userStory.getEstimationType() == EstimationType.STORY_POINTS) {
            // For story points, use the median of votes
            int finalEstimate = calculateFinalStoryPoints(votes);
            userStory.setStoryPoints(finalEstimate);
        } else if (userStory.getEstimationType() == EstimationType.PERT) {
            // For PERT, calculate optimistic, most likely, and pessimistic estimates
            calculatePERTEstimates(votes, userStory);
        }

        userStoryRepository.save(userStory);
    }

    private int calculateFinalStoryPoints(List<EstimationVote> votes) {
        // Calculate median of story points
        List<Integer> storyPoints = votes.stream()
            .map(EstimationVote::getStoryPoints)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());

        if (storyPoints.isEmpty()) {
            return 0;
        }

        // Use median for final estimate
        int middle = storyPoints.size() / 2;
        if (storyPoints.size() % 2 == 1) {
            return storyPoints.get(middle);
        } else {
            return (storyPoints.get(middle - 1) + storyPoints.get(middle)) / 2;
        }
    }

    private void calculatePERTEstimates(List<EstimationVote> votes, UserStory userStory) {
        // Calculate optimistic, most likely, and pessimistic estimates from votes
        OptionalDouble optimisticOpt = votes.stream()
            .mapToDouble(EstimationVote::getStoryPoints)
            .filter(p -> p > 0)
            .min();

        OptionalDouble pessimisticOpt = votes.stream()
            .mapToDouble(EstimationVote::getStoryPoints)
            .filter(p -> p > 0)
            .max();

        OptionalDouble mostLikelyOpt = votes.stream()
            .mapToDouble(EstimationVote::getStoryPoints)
            .filter(p -> p > 0)
            .average();

        if (optimisticOpt.isPresent() && pessimisticOpt.isPresent() && mostLikelyOpt.isPresent()) {
            double optimistic = optimisticOpt.getAsDouble();
            double pessimistic = pessimisticOpt.getAsDouble();
            double mostLikely = mostLikelyOpt.getAsDouble();

            userStory.setOptimisticEstimate(optimistic);
            userStory.setPessimisticEstimate(pessimistic);
            userStory.setMostLikelyEstimate(mostLikely);

            // Calculate PERT estimate: (O + 4M + P) / 6
            double pertEstimate = (optimistic + (4 * mostLikely) + pessimistic) / 6;
            userStory.setPertEstimate(pertEstimate);

            // Calculate variance: ((P - O) / 6)²
            double variance = Math.pow((pessimistic - optimistic) / 6, 2);
            userStory.setVariance(variance);

            // Set story points as rounded PERT estimate
            userStory.setStoryPoints((int) Math.round(pertEstimate));
        }
    }
}
