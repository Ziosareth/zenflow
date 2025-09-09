package it.zenflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Read/View DTO for UserStory to be used by the web layer.
 * Contains only view-safe primitives/Strings to avoid touching JPA proxies in templates.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStoryViewDTO {
    private Long id;
    private String title;
    private String description;
    private String acceptanceCriteria;

    // Enum names pre-resolved to avoid comparing enum instances in templates
    private String statusName;    // e.g., BACKLOG, IN_PROGRESS, DONE
    private String priorityName;  // e.g., LOW, MEDIUM, HIGH

    private Integer storyPoints;
    private Integer businessValue;

    // Estimation fields (copy of entity numeric data)
    private String estimationTypeName;
    private Double optimisticEstimate;
    private Double pessimisticEstimate;
    private Double mostLikelyEstimate;
    private Double pertEstimate;
    private Double variance;

    // Flattened relation fields
    private Long projectId;
    private Long sprintId;
    private String assignedToUsername; // null-safe username for assignee

    // Aggregates for view logic
    private Integer tasksCount;
}
