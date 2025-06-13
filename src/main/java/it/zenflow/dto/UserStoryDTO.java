package it.zenflow.dto;

import it.zenflow.model.project.enums.EstimationType;
import it.zenflow.model.project.enums.Priority;
import it.zenflow.model.project.enums.StoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStoryDTO {

    private Long id;

    @NotBlank(message = "{validation.userstory.title.required}")
    @Size(min = 3, max = 200, message = "{validation.userstory.title.size}")
    private String title;

    @Size(max = 2000, message = "{validation.userstory.description.size}")
    private String description;

    @Size(max = 2000, message = "{validation.userstory.acceptanceCriteria.size}")
    private String acceptanceCriteria;

    @NotNull(message = "{validation.userstory.status.required}")
    private StoryStatus status = StoryStatus.BACKLOG;

    @NotNull(message = "{validation.userstory.priority.required}")
    private Priority priority = Priority.MEDIUM;

    private Integer storyPoints;

    private Integer businessValue;

    @NotNull(message = "{validation.userstory.estimationType.required}")
    private EstimationType estimationType = EstimationType.STORY_POINTS;

    private Long projectId;

    private Long sprintId;

    private Long assignedToId;

    // PERT Estimation fields
    private Double optimisticEstimate;
    private Double pessimisticEstimate;
    private Double mostLikelyEstimate;
    private Double pertEstimate;
    private Double variance;
}
