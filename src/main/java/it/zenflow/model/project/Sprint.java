package it.zenflow.model.project;

import it.zenflow.model.project.enums.SprintStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sprints")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sprint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String goal;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SprintStatus status = SprintStatus.PLANNED;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL)
    private List<UserStory> stories = new ArrayList<>();
    
    // Metriche Sprint
    @Column(name = "planned_story_points")
    private Integer plannedStoryPoints = 0;
    
    @Column(name = "completed_story_points")
    private Integer completedStoryPoints = 0;
    
    @Column(name = "sprint_velocity")
    private Double sprintVelocity = 0.0;
}

