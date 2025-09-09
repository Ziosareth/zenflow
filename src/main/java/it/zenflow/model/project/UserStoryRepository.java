package it.zenflow.model.project;

import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    List<UserStory> findByProject(Project project);

    @Query("SELECT us FROM UserStory us LEFT JOIN FETCH us.assignedTo WHERE us.project = :project")
    List<UserStory> findByProjectWithAssignedUser(@Param("project") Project project);
    
    @EntityGraph(attributePaths = {"assignedTo"})
    @Query("SELECT us FROM UserStory us WHERE us.project = :project")
    Page<UserStory> findByProjectPaginated(@Param("project") Project project, Pageable pageable);

    List<UserStory> findByProjectAndStatus(Project project, StoryStatus status);

    List<UserStory> findByAssignedTo(User user);

    @Query("SELECT us FROM UserStory us WHERE us.project.id = :projectId")
    List<UserStory> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT us FROM UserStory us WHERE us.project.id = :projectId AND us.status = :status")
    List<UserStory> findByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") StoryStatus status);

    @Query("SELECT us FROM UserStory us LEFT JOIN FETCH us.tasks LEFT JOIN FETCH us.assignedTo WHERE us.id = :id")
    Optional<UserStory> findByIdWithTasks(@Param("id") Long id);

    @Query("SELECT us FROM UserStory us JOIN FETCH us.project WHERE us.id = :id")
    Optional<UserStory> findByIdWithProject(@Param("id") Long id);
    
    @Query("SELECT us FROM UserStory us JOIN FETCH us.project LEFT JOIN FETCH us.sprint WHERE us.id = :id")
    Optional<UserStory> findByIdWithProjectAndSprint(@Param("id") Long id);

    List<UserStory> findByProjectAndSprintIsNull(Project project);
}
