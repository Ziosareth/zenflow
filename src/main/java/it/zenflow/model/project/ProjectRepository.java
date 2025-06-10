package it.zenflow.model.project;

import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.rbac.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    List<Project> findByStatus(ProjectStatus status);
    
    List<Project> findByOwner(User owner);
    
    @Query("SELECT p FROM Project p WHERE p.status = :status AND " +
           "(p.owner = :user OR :user MEMBER OF p.teamMembers)")
    List<Project> findUserProjects(@Param("user") User user, @Param("status") ProjectStatus status);
    
    @Query("SELECT p FROM Project p WHERE :user MEMBER OF p.teamMembers")
    List<Project> findProjectsByTeamMember(@Param("user") User user);
}