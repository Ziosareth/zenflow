package it.zenflow.mapper;

import it.zenflow.dto.UserStoryDTO;
import it.zenflow.model.project.UserStory;
import org.mapstruct.*;

/**
 * Mapper for the UserStory entity and its DTO.
 */
@Mapper(componentModel = "spring")
public abstract class UserStoryMapper {

    /**
     * Converts a UserStoryDTO to a UserStory entity.
     * Relationships are loaded via fetch joins in repository queries.
     */
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "planningSessions", ignore = true)
    public abstract UserStory toEntity(UserStoryDTO dto);

    /**
     * Converts a UserStory entity to a UserStoryDTO.
     * Maps IDs from related entities.
     */
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "sprintId", source = "sprint.id")
    @Mapping(target = "assignedToId", source = "assignedTo.id")
    public abstract UserStoryDTO toDto(UserStory userStory);

    /**
     * Updates a UserStory entity with data from a UserStoryDTO.
     * Ignores null values and relationships that are loaded via fetch joins in repository queries.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "planningSessions", ignore = true)
    public abstract void updateEntityFromDto(UserStoryDTO dto, @MappingTarget UserStory userStory);

}