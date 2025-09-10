package it.zenflow.model.metrics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailySnapshotRepository extends JpaRepository<DailySnapshot, Long> {

    @Query("select s from DailySnapshot s where s.sprint.id = :sprintId and s.date between :start and :end order by s.date")
    List<DailySnapshot> findBySprintAndRange(@Param("sprintId") Long sprintId,
                                             @Param("start") LocalDate start,
                                             @Param("end") LocalDate end);

    Optional<DailySnapshot> findByDateAndSprint(LocalDate date, it.zenflow.model.project.Sprint sprint);
}
