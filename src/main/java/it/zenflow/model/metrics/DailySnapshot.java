package it.zenflow.model.metrics;

import it.zenflow.model.project.Sprint;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

@Entity
@Table(name = "daily_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"date", "sprint_id"}))
@Audited
@Getter
@Setter
@NoArgsConstructor
public class DailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @Column(name = "scope_total")
    private Integer scopeTotal;

    @Column(name = "remaining")
    private Integer remaining;
}
