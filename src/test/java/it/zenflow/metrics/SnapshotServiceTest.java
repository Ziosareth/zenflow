package it.zenflow.metrics;

import it.zenflow.model.metrics.DailySnapshot;
import it.zenflow.model.metrics.DailySnapshotRepository;
import it.zenflow.model.project.Project;
import it.zenflow.model.project.Sprint;
import it.zenflow.model.project.UserStory;
import it.zenflow.model.project.enums.SprintStatus;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SnapshotServiceTest {

    private DailySnapshotRepository snapshotRepository;
    private it.zenflow.model.project.SprintRepository sprintRepository;

    @BeforeEach
    void setup() {
        snapshotRepository = mock(DailySnapshotRepository.class);
        sprintRepository = mock(it.zenflow.model.project.SprintRepository.class);
    }

    @Test
    void snapshotSprint_shouldComputeScopeAndRemaining_andSaveNewSnapshot() {
        // given
        SnapshotService service = new SnapshotService(snapshotRepository, sprintRepository);

        Sprint sprint = buildSprint(1L, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 5));
        // stories: 3 + 5 + null; one DONE(5), one IN_PROGRESS(3), one BACKLOG(null)
        sprint.setStories(new ArrayList<>(List.of(
                story(3, StoryStatus.IN_PROGRESS),
                story(5, StoryStatus.DONE),
                story(null, StoryStatus.SELECTED)
        )));

        when(sprintRepository.findByIdWithStories(1L)).thenReturn(Optional.of(sprint));
        when(snapshotRepository.findByDateAndSprint(any(LocalDate.class), any(Sprint.class)))
                .thenReturn(Optional.empty());

        LocalDate date = LocalDate.of(2025, 9, 2);

        // when
        service.snapshotSprint(1L, date);

        // then
        ArgumentCaptor<DailySnapshot> captor = ArgumentCaptor.forClass(DailySnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        DailySnapshot saved = captor.getValue();

        assertThat(saved.getDate()).isEqualTo(date);
        assertThat(saved.getSprint()).isEqualTo(sprint);
        // scope should sum non-null points: 3 + 5 = 8
        assertThat(saved.getScopeTotal()).isEqualTo(8);
        // remaining excludes DONE(5), so only 3 remains
        assertThat(saved.getRemaining()).isEqualTo(3);
    }

    @Test
    void snapshotSprint_shouldUpdateExistingSnapshot() {
        // given
        SnapshotService service = new SnapshotService(snapshotRepository, sprintRepository);

        Sprint sprint = buildSprint(2L, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 5));
        sprint.setStories(new ArrayList<>(List.of(
                story(2, StoryStatus.DONE),
                story(2, StoryStatus.DONE)
        )));

        DailySnapshot existing = new DailySnapshot();
        existing.setSprint(sprint);
        existing.setDate(LocalDate.of(2025, 9, 3));
        existing.setScopeTotal(99);
        existing.setRemaining(99);

        when(sprintRepository.findByIdWithStories(2L)).thenReturn(Optional.of(sprint));
        when(snapshotRepository.findByDateAndSprint(eq(existing.getDate()), eq(sprint)))
                .thenReturn(Optional.of(existing));

        // when
        service.snapshotSprint(2L, existing.getDate());

        // then
        verify(snapshotRepository).save(existing);
        assertThat(existing.getScopeTotal()).isEqualTo(4);
        assertThat(existing.getRemaining()).isEqualTo(0);
    }

    @Test
    void snapshotActiveSprintsAt_shouldSnapshotOnlyActiveSprintsInclusive() {
        // given
        SnapshotService realService = new SnapshotService(snapshotRepository, sprintRepository);
        SnapshotService service = Mockito.spy(realService);

        Sprint s1 = buildSprint(10L, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 3)); // active
        Sprint s2 = buildSprint(11L, LocalDate.of(2025, 9, 4), LocalDate.of(2025, 9, 10)); // not active on date
        Sprint s3 = buildSprint(12L, LocalDate.of(2025, 8, 30), LocalDate.of(2025, 9, 2)); // active

        when(sprintRepository.findAll()).thenReturn(List.of(s1, s2, s3));

        LocalDate date = LocalDate.of(2025, 9, 2);

        // stub snapshotSprint to avoid deeper logic, just verify it's called
        doNothing().when(service).snapshotSprint(anyLong(), any(LocalDate.class));

        // when
        service.snapshotActiveSprintsAt(date);

        // then
        verify(service, times(1)).snapshotSprint(eq(10L), eq(date));
        verify(service, never()).snapshotSprint(eq(11L), any());
        verify(service, times(1)).snapshotSprint(eq(12L), eq(date));
    }

    private Sprint buildSprint(Long id, LocalDate start, LocalDate end) {
        Sprint s = new Sprint();
        s.setId(id);
        s.setName("S-" + id);
        s.setGoal("g");
        s.setStartDate(start);
        s.setEndDate(end);
        s.setStatus(SprintStatus.ACTIVE);
        // minimal required relations
        Project p = new Project();
        p.setId(1L);
        s.setProject(p);
        s.setStories(new ArrayList<>());
        return s;
    }

    private UserStory story(Integer points, StoryStatus status) {
        UserStory us = new UserStory();
        us.setId(null);
        us.setTitle("t");
        us.setDescription("d");
        us.setAcceptanceCriteria("a");
        us.setStatus(status);
        us.setStoryPoints(points);
        us.setProject(new Project());
        us.setAssignedTo(new User());
        return us;
    }
}
