package it.zenflow.model.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProject(Project project);
    
    @Query("SELECT DISTINCT s FROM Sprint s LEFT JOIN FETCH s.stories st LEFT JOIN FETCH st.assignedTo WHERE s.id = :id")
    Optional<Sprint> findByIdWithStories(@Param("id") Long id);
    
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId ORDER BY s.startDate DESC")
    List<Sprint> findByProjectIdOrderByStartDateDesc(@Param("projectId") Long projectId);
    
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'COMPLETED'")
    List<Sprint> findCompletedSprintsByProjectId(@Param("projectId") Long projectId);
}