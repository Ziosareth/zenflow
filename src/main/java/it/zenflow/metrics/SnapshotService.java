package it.zenflow.metrics;

import it.zenflow.model.metrics.DailySnapshot;
import it.zenflow.model.metrics.DailySnapshotRepository;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.SprintRepository;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.StoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
class SnapshotService {

    private final DailySnapshotRepository snapshotRepository;
    private final SprintRepository sprintRepository;

    @Transactional(transactionManager = "tenantTransactionManager")
    public void snapshotActiveSprintsAt(LocalDate date) {
        // Find sprints active by dates (inclusive)
        List<Sprint> all = sprintRepository.findAll();
        all.stream()
                .filter(s -> !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                .forEach(s -> snapshotSprint(s.getId(), date));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void snapshotSprint(Long sprintId, LocalDate date) {
        // Load sprint with stories to avoid Lazy issues
        Sprint sprint = sprintRepository.findByIdWithStories(sprintId).orElseThrow();

        int scope = sprint.getStories().stream()
                .filter(story -> story.getStoryPoints() != null)
                .mapToInt(UserStory::getStoryPoints)
                .sum();

        int remaining = sprint.getStories().stream()
                .filter(story -> story.getStoryPoints() != null && story.getStatus() != StoryStatus.DONE)
                .mapToInt(UserStory::getStoryPoints)
                .sum();

        DailySnapshot snapshot = snapshotRepository.findByDateAndSprint(date, sprint)
                .orElseGet(DailySnapshot::new);
        snapshot.setDate(date);
        snapshot.setSprint(sprint);
        snapshot.setScopeTotal(scope);
        snapshot.setRemaining(remaining);
        snapshotRepository.save(snapshot);
    }
}
