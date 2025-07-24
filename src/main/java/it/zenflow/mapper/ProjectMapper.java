package it.zenflow.mapper;

import it.zenflow.dto.ProjectDTO;
import it.zenflow.model.project.Project;
import it.zenflow.model.rbac.User;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for the Project entity and its DTO.
 */
@Mapper(componentModel = "spring")
public interface ProjectMapper {

    /**
     * Converts a ProjectDTO to a Project entity.
     * Ignores fields that should be set separately (team members, owner, etc.).
     */
    @Mapping(target = "teamMembers", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "sprints", ignore = true)
    @Mapping(target = "backlog", ignore = true)
    @Mapping(target = "totalStoryPoints", ignore = true)
    @Mapping(target = "completedStoryPoints", ignore = true)
    @Mapping(target = "teamVelocity", ignore = true)
    Project toEntity(ProjectDTO dto);

    /**
     * Converts a Project entity to a ProjectDTO.
     * Maps team member IDs from the team members collection.
     */
    @Mapping(target = "teamMemberIds", expression = "java(getTeamMemberIds(project))")
    ProjectDTO toDto(Project project);

    /**
     * Updates a Project entity with data from a ProjectDTO.
     * Ignores null values and fields that should be updated separately.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teamMembers", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "sprints", ignore = true)
    @Mapping(target = "backlog", ignore = true)
    @Mapping(target = "totalStoryPoints", ignore = true)
    @Mapping(target = "completedStoryPoints", ignore = true)
    @Mapping(target = "teamVelocity", ignore = true)
    void updateEntityFromDto(ProjectDTO dto, @MappingTarget Project project);

    /**
     * Helper method to extract team member IDs from the team members collection.
     */
    default Set<Long> getTeamMemberIds(Project project) {
        if (project.getTeamMembers() == null) {
            return java.util.Collections.emptySet();
        }
        return project.getTeamMembers().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }
}