package it.zenflow.model.project;

import it.zenflow.model.rbac.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "estimation_votes")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimationVote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PlanningPokerSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_story_id", nullable = false)
    private UserStory userStory;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;
    
    @Column(name = "story_points")
    private Integer storyPoints;
    
    @Column(columnDefinition = "TEXT")
    private String reasoning;
}