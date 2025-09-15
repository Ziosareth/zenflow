package it.zenflow.model.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    @Query("SELECT m FROM Milestone m WHERE m.project = :project ORDER BY m.targetDate ASC")
    List<Milestone> findByProject(@Param("project") Project project);
}
