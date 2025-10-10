package it.zenflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * DTO for representing items in the project Gantt chart.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GanttItemDTO(
        String id,
        String name,
        LocalDate start,
        LocalDate end,
        int progress,
        String type,
        String parentId
) {}
