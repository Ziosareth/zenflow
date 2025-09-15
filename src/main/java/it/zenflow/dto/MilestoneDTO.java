package it.zenflow.dto;

import it.zenflow.model.project.enums.MilestoneStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class MilestoneDTO {

    @NotNull
    private Long projectId;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 4000)
    private String description;

    private MilestoneStatus status; // optional in form, default to PLANNED

    @NotNull
    @FutureOrPresent
    private LocalDate targetDate;

    private LocalDate achievedDate; // optional

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public MilestoneStatus getStatus() { return status; }
    public void setStatus(MilestoneStatus status) { this.status = status; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public LocalDate getAchievedDate() { return achievedDate; }
    public void setAchievedDate(LocalDate achievedDate) { this.achievedDate = achievedDate; }
}
