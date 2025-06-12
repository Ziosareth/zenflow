package it.zenflow.dto;

import it.zenflow.model.project.enums.ProjectStatus;
import it.zenflow.model.project.enums.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    
    private Long id;
    
    @NotBlank(message = "{validation.project.name.required}")
    @Size(min = 3, max = 100, message = "{validation.project.name.size}")
    private String name;
    
    @Size(max = 500, message = "{validation.project.description.size}")
    private String description;
    
    @NotNull(message = "{validation.project.status.required}")
    private ProjectStatus status = ProjectStatus.ACTIVE;
    
    @NotNull(message = "{validation.project.type.required}")
    private ProjectType type = ProjectType.SCRUM;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    // Team members IDs for form binding
    private Set<Long> teamMemberIds = new HashSet<>();
}