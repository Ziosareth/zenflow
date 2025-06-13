package it.zenflow.model.project;

import it.zenflow.model.project.enums.SessionStatus;
import it.zenflow.model.rbac.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanningPokerSessionRepository extends JpaRepository<PlanningPokerSession, Long> {

    List<PlanningPokerSession> findByProject(Project project);

    List<PlanningPokerSession> findByProjectId(Long projectId);

    List<PlanningPokerSession> findByFacilitator(User facilitator);

    List<PlanningPokerSession> findByStatus(SessionStatus status);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.participants WHERE s.id = :id")
    Optional<PlanningPokerSession> findByIdWithParticipants(@Param("id") Long id);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.votes WHERE s.id = :id")
    Optional<PlanningPokerSession> findByIdWithVotes(@Param("id") Long id);

    @Query("SELECT s FROM PlanningPokerSession s LEFT JOIN FETCH s.facilitator LEFT JOIN FETCH s.participants LEFT JOIN FETCH s.votes WHERE s.id = :id")
    Optional<PlanningPokerSession> findByIdWithParticipantsAndVotes(@Param("id") Long id);
}
