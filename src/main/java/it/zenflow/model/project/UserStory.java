package it.zenflow.model.project;

import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.StoryStatus;
import it.zenflow.model.rbac.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_stories")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoryStatus status = StoryStatus.BACKLOG;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;
    
    @Column(name = "story_points")
    private Integer storyPoints;
    
    @Column(name = "business_value")
    private Integer businessValue;
    
    // Relazioni
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
    
    @OneToMany(mappedBy = "userStory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
    
    // PERT Estimation
    @Column(name = "optimistic_estimate")
    private Double optimisticEstimate;
    
    @Column(name = "pessimistic_estimate")
    private Double pessimisticEstimate;
    
    @Column(name = "most_likely_estimate")
    private Double mostLikelyEstimate;
    
    @Column(name = "pert_estimate")
    private Double pertEstimate;
}



