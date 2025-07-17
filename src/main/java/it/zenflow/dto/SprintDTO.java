package it.zenflow.dto;

import it.zenflow.model.project.enums.SprintStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class SprintDTO {
    private Long id;
    
    @NotBlank(message = "{sprint.name.required}")
    private String name;
    
    private String goal;
    
    @NotNull(message = "{sprint.startDate.required}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @NotNull(message = "{sprint.endDate.required}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private SprintStatus status;
    
    private Long projectId;
    
    private Integer plannedStoryPoints = 0;
    private Integer completedStoryPoints = 0;
    private Double sprintVelocity = 0.0;
}