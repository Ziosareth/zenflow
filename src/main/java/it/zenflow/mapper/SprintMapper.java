package it.zenflow.mapper;

import it.zenflow.dto.SprintDTO;
import it.zenflow.model.project.Sprint;
import org.mapstruct.*;

/**
 * Mapper for the Sprint entity and its DTO.
 */
@Mapper(componentModel = "spring")
public interface SprintMapper {

    /**
     * Converts a SprintDTO to a Sprint entity.
     * Relationships are loaded via fetch joins in repository queries.
     */
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "stories", ignore = true)
    Sprint toEntity(SprintDTO dto);

    /**
     * Converts a Sprint entity to a SprintDTO.
     * Maps IDs from related entities.
     */
    @Mapping(target = "projectId", source = "project.id")
    SprintDTO toDto(Sprint sprint);

    /**
     * Updates a Sprint entity with data from a SprintDTO.
     * Ignores null values and relationships that are loaded via fetch joins in repository queries.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "stories", ignore = true)
    @Mapping(target = "status", ignore = true) // Status is managed by specific methods
    void updateEntityFromDto(SprintDTO dto, @MappingTarget Sprint sprint);
}