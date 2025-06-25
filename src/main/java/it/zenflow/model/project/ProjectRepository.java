package it.zenflow.model.project;

import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.rbac.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.owner LEFT JOIN FETCH p.teamMembers WHERE p.status = :status")
    List<Project> findByStatus(@Param("status") ProjectStatus status);

    // Custom query to eagerly load owner and team members
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.owner LEFT JOIN FETCH p.teamMembers WHERE p.owner = :owner")
    List<Project> findByOwner(@Param("owner") User owner);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.owner LEFT JOIN FETCH p.teamMembers WHERE p.status = :status AND " +
           "(p.owner = :user OR :user MEMBER OF p.teamMembers)")
    List<Project> findUserProjects(@Param("user") User user, @Param("status") ProjectStatus status);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.owner LEFT JOIN FETCH p.teamMembers WHERE :user MEMBER OF p.teamMembers")
    List<Project> findProjectsByTeamMember(@Param("user") User user);

    // Add fetch join query to eagerly load owner and team members
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.owner LEFT JOIN FETCH p.teamMembers")
    List<Project> findAllWithOwners();

    // For pagination, we need two queries
    @Query(value = "SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.owner LEFT JOIN FETCH p.teamMembers WHERE p.id IN :ids",
           countQuery = "SELECT COUNT(p) FROM Project p")
    List<Project> findByIdInWithOwners(@Param("ids") List<Long> ids);

    // Fetch a single project by ID with owner and team members eagerly loaded
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.owner LEFT JOIN FETCH p.teamMembers WHERE p.id = :id")
    Optional<Project> findByIdWithOwner(@Param("id") Long id);

    // Fetch projects by owner with owner and team members eagerly loaded
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.owner LEFT JOIN FETCH p.teamMembers WHERE p.owner = :owner")
    List<Project> findByOwnerWithOwner(@Param("owner") User owner);
}
