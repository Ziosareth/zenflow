package it.zenflow.model.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpicRepository extends JpaRepository<Epic, Long> {

    @Query("SELECT e FROM Epic e WHERE e.project = :project ORDER BY e.startDate ASC NULLS LAST, e.id ASC")
    List<Epic> findByProject(@Param("project") Project project);
}
