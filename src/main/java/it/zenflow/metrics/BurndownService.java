package it.zenflow.metrics;

import it.zenflow.metrics.dto.BurndownResponse;
import it.zenflow.model.metrics.DailySnapshot;
import it.zenflow.model.metrics.DailySnapshotRepository;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.SprintRepository;
import it.zenflow.model.project.UserStory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BurndownService {

    private final DailySnapshotRepository snapshotRepository;
    private final SprintRepository sprintRepository;

    @Transactional(readOnly = true, transactionManager = "tenantTransactionManager")
    public BurndownResponse getSprintBurndown(Long sprintId) {
        Sprint sprint = sprintRepository.findByIdWithStories(sprintId).orElseThrow();
        LocalDate start = sprint.getStartDate();
        LocalDate end = sprint.getEndDate();

        List<LocalDate> days = start.datesUntil(end.plusDays(1)).toList();
        List<DailySnapshot> snaps = snapshotRepository.findBySprintAndRange(sprintId, start, end);
        Map<LocalDate, DailySnapshot> byDate = snaps.stream()
                .collect(Collectors.toMap(DailySnapshot::getDate, s -> s));

        int initialScope = Optional.ofNullable(byDate.get(start))
                .map(DailySnapshot::getScopeTotal)
                .orElseGet(() -> sprint.getStories().stream()
                        .filter(st -> st.getStoryPoints() != null)
                        .mapToInt(UserStory::getStoryPoints)
                        .sum());

        int daysCount = Math.max(1, (int) ChronoUnit.DAYS.between(start, end) + 1);
        List<Integer> ideal = new ArrayList<>(days.size());
        List<Integer> remaining = new ArrayList<>(days.size());
        List<Integer> scope = new ArrayList<>(days.size());

        for (int i = 0; i < days.size(); i++) {
            LocalDate d = days.get(i);
            double fraction = daysCount == 1 ? 1.0 : (double) i / (daysCount - 1);
            int idealRemaining = (int) Math.round(initialScope * (1 - fraction));
            ideal.add(Math.max(idealRemaining, 0));

            DailySnapshot s = byDate.get(d);
            if (s != null) {
                remaining.add(Optional.ofNullable(s.getRemaining()).orElse(0));
                scope.add(Optional.ofNullable(s.getScopeTotal()).orElse(initialScope));
            } else {
                int lastRemaining = remaining.isEmpty() ? initialScope : remaining.getLast();
                int lastScope = scope.isEmpty() ? initialScope : scope.getLast();
                remaining.add(lastRemaining);
                scope.add(lastScope);
            }
        }

        return new BurndownResponse(start, end, days, ideal, remaining, scope, "story_points");
    }
}
