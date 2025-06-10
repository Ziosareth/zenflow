package it.zenflow.model.project;

import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import it.zenflow.model.rbac.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "projects")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectType type = ProjectType.SCRUM;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    // Relazioni
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "project_team_members",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> teamMembers = new HashSet<>();
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sprint> sprints = new ArrayList<>();
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserStory> backlog = new ArrayList<>();
    
    // Metriche
    @Column(name = "total_story_points")
    private Integer totalStoryPoints = 0;
    
    @Column(name = "completed_story_points")
    private Integer completedStoryPoints = 0;
    
    @Column(name = "team_velocity")
    private Double teamVelocity = 0.0;
}