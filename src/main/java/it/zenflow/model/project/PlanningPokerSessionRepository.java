package it.zenflow.model.project;

import it.zenflow.model.project.enums.SessionStatus;
import it.zenflow.model.rbac.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanningPokerSessionRepository extends JpaRepository<PlanningPokerSession, Long> {

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants LEFT JOIN FETCH s.project")
    List<PlanningPokerSession> findAll();

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants WHERE s.project = :project")
    List<PlanningPokerSession> findByProject(@Param("project") Project project);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants WHERE s.project.id = :projectId")
    List<PlanningPokerSession> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants WHERE s.facilitator = :facilitator")
    List<PlanningPokerSession> findByFacilitator(@Param("facilitator") User facilitator);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants WHERE s.status = :status")
    List<PlanningPokerSession> findByStatus(@Param("status") SessionStatus status);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants WHERE s.id = :id")
    Optional<PlanningPokerSession> findByIdWithParticipants(@Param("id") Long id);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants LEFT JOIN FETCH s.votes WHERE s.id = :id")
    Optional<PlanningPokerSession> findByIdWithVotes(@Param("id") Long id);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants LEFT JOIN FETCH s.votes LEFT JOIN FETCH s.project LEFT JOIN FETCH s.userStory WHERE s.id = :id")
    Optional<PlanningPokerSession> findByIdWithParticipantsAndVotes(@Param("id") Long id);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants WHERE s.userStory = :userStory")
    List<PlanningPokerSession> findByUserStory(@Param("userStory") UserStory userStory);
    
    @Query("SELECT DISTINCT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants LEFT JOIN FETCH s.project")
    Page<PlanningPokerSession> findAllPaginated(Pageable pageable);

    @Query("SELECT DISTINCT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants WHERE s.project.id = :projectId")
    Page<PlanningPokerSession> findByProjectIdPaginated(@Param("projectId") Long projectId, Pageable pageable);
}
