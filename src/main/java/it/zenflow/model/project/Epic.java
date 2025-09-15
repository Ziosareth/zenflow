package it.zenflow.model.project;

import it.zenflow.model.project.enums.EpicStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "epics",
       indexes = {
         @Index(name = "idx_epic_project", columnList = "project_id"),
         @Index(name = "idx_epic_status", columnList = "status")
       })
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Epic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Facoltativo: codice breve univoco per report/ricerche (es. "EP-42")
    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EpicStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Lato inverso della relazione con Milestone
    @ManyToMany(mappedBy = "epics")
    @Builder.Default
    private Set<Milestone> milestones = new LinkedHashSet<>();

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = EpicStatus.PLANNED;
        }
    }
}
