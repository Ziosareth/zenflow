package it.zenflow.metrics;

import it.zenflow.metrics.dto.BurndownResponse;
import it.zenflow.model.metrics.DailySnapshot;
import it.zenflow.model.metrics.DailySnapshotRepository;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.SprintStatus;
import it.zenflow.model.project.enums.StoryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BurndownServiceTest {

    private DailySnapshotRepository snapshotRepository;
    private it.zenflow.model.project.SprintRepository sprintRepository;

    @BeforeEach
    void setup() {
        snapshotRepository = mock(DailySnapshotRepository.class);
        sprintRepository = mock(it.zenflow.model.project.SprintRepository.class);
    }

    @Test
    void getSprintBurndown_shouldComputeFromSnapshotsAndCarryForwardMissingDays() {
        // Sprint 3 days: 1..3
        long sprintId = 100L;
        LocalDate start = LocalDate.of(2025, 9, 1);
        LocalDate end = LocalDate.of(2025, 9, 3);

        Sprint sprint = buildSprint(sprintId, start, end, 8 /*story points from stories*/);

        when(sprintRepository.findByIdWithStories(sprintId)).thenReturn(Optional.of(sprint));

        // Snapshots for day 1 and day 3 (missing day 2)
        DailySnapshot d1 = snap(sprint, start, 8, 6);
        DailySnapshot d3 = snap(sprint, end, 9, 0); // scope increased by 1 on last day
        when(snapshotRepository.findBySprintAndRange(eq(sprintId), eq(start), eq(end)))
                .thenReturn(List.of(d1, d3));

        BurndownService service = new BurndownService(snapshotRepository, sprintRepository);

        // when
        BurndownResponse resp = service.getSprintBurndown(sprintId);

        // then
        assertThat(resp.startDate()).isEqualTo(start);
        assertThat(resp.endDate()).isEqualTo(end);
        assertThat(resp.labels()).containsExactly(start, start.plusDays(1), end);

        // initialScope should be from snapshot day1 -> scopeTotal=8
        // ideal should go 8 -> 4 -> 0
        assertThat(resp.ideal()).containsExactly(8, 4, 0);

        // remaining: day1=6, day2 carry-forward=6, day3=0
        assertThat(resp.remaining()).containsExactly(6, 6, 0);

        // scope: day1=8, day2 carry-forward=8, day3=9
        assertThat(resp.scope()).containsExactly(8, 8, 9);

        assertThat(resp.unit()).isEqualTo("story_points");
    }

    @Test
    void getSprintBurndown_shouldDeriveInitialScopeFromStoriesWhenNoStartSnapshot() {
        long sprintId = 101L;
        LocalDate start = LocalDate.of(2025, 9, 1);
        LocalDate end = LocalDate.of(2025, 9, 2); // 2 days

        // Two stories 3+2 = 5; one DONE, one IN_PROGRESS to simulate remaining on day2 snapshot
        Sprint sprint = buildSprint(sprintId, start, end, 5);
        when(sprintRepository.findByIdWithStories(sprintId)).thenReturn(Optional.of(sprint));

        // Only snapshot on day 2, no snapshot day 1
        DailySnapshot d2 = new DailySnapshot();
        d2.setSprint(sprint);
        d2.setDate(start.plusDays(1));
        d2.setScopeTotal(5);
        d2.setRemaining(2);

        when(snapshotRepository.findBySprintAndRange(eq(sprintId), eq(start), eq(end)))
                .thenReturn(List.of(d2));

        BurndownService service = new BurndownService(snapshotRepository, sprintRepository);

        BurndownResponse resp = service.getSprintBurndown(sprintId);

        // ideal over 2 days: 5 -> 0
        assertThat(resp.ideal()).containsExactly(5, 0);
        // remaining day1 should be initialScope (carry-forward from none -> initial), day2=2
        assertThat(resp.remaining()).containsExactly(5, 2);
    }

    @Test
    void getSprintBurndown_singleDaySprint_shouldIdealEqualInitialScopeAndZero() {
        long sprintId = 102L;
        LocalDate day = LocalDate.of(2025, 9, 10);
        Sprint sprint = buildSprint(sprintId, day, day, 7);
        when(sprintRepository.findByIdWithStories(sprintId)).thenReturn(Optional.of(sprint));

        // Provide a snapshot for the same day to set remaining = 3
        DailySnapshot ds = snap(sprint, day, 7, 3);
        when(snapshotRepository.findBySprintAndRange(eq(sprintId), eq(day), eq(day)))
                .thenReturn(List.of(ds));

        BurndownService service = new BurndownService(snapshotRepository, sprintRepository);
        BurndownResponse resp = service.getSprintBurndown(sprintId);

        // With single day, daysCount==1 -> fraction=1.0, idealRemaining = round(7*(1-1))=0
        assertThat(resp.labels()).containsExactly(day);
        assertThat(resp.ideal()).containsExactly(0);
        assertThat(resp.remaining()).containsExactly(3);
        assertThat(resp.scope()).containsExactly(7);
    }

    private Sprint buildSprint(Long id, LocalDate start, LocalDate end, int totalStoryPoints) {
        Sprint s = new Sprint();
        s.setId(id);
        s.setName("Sprint " + id);
        s.setGoal("g");
        s.setStartDate(start);
        s.setEndDate(end);
        s.setStatus(SprintStatus.ACTIVE);
        Project p = new Project();
        p.setId(1L);
        s.setProject(p);
        List<UserStory> stories = new ArrayList<>();
        int remaining = totalStoryPoints;
        while (remaining > 0) {
            int add = Math.min(3, remaining);
            UserStory us = new UserStory();
            us.setTitle("t");
            us.setStoryPoints(add);
            us.setStatus(StoryStatus.SELECTED);
            us.setProject(p);
            stories.add(us);
            remaining -= add;
        }
        s.setStories(stories);
        return s;
    }

    private DailySnapshot snap(Sprint sprint, LocalDate date, Integer scope, Integer remaining) {
        DailySnapshot ds = new DailySnapshot();
        ds.setSprint(sprint);
        ds.setDate(date);
        ds.setScopeTotal(scope);
        ds.setRemaining(remaining);
        return ds;
    }
}
