package it.zenflow.model.project;

import it.zenflow.model.project.enums.MilestoneStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "milestones",
       indexes = {
         @Index(name = "idx_milestone_project", columnList = "project_id"),
         @Index(name = "idx_milestone_target_date", columnList = "target_date")
       })
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MilestoneStatus status;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "achieved_date")
    private LocalDate achievedDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToMany
    @JoinTable(
        name = "milestone_epics",
        joinColumns = @JoinColumn(name = "milestone_id"),
        inverseJoinColumns = @JoinColumn(name = "epic_id"),
        indexes = {
            @Index(name = "idx_milestone_epics_milestone", columnList = "milestone_id"),
            @Index(name = "idx_milestone_epics_epic", columnList = "epic_id")
        }
    )
    @Builder.Default
    private Set<Epic> epics = new LinkedHashSet<>();

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = MilestoneStatus.PLANNED;
        }
    }

    @Transient
    public boolean isAchieved() {
        return MilestoneStatus.ACHIEVED.equals(this.status);
    }
}
