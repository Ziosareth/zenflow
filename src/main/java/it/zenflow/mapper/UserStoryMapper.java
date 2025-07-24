package it.zenflow.mapper;

import it.zenflow.dto.UserStoryDTO;
import it.zenflow.model.project.UserStory;
import it.zenflow.service.ProjectService;
import it.zenflow.service.SprintService;
import it.zenflow.service.rbac.UserService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mapper for the UserStory entity and its DTO.
 * Uses services to resolve entity references.
 */
@Mapper(componentModel = "spring")
public abstract class UserStoryMapper {

    @Autowired
    protected ProjectService projectService;
    
    @Autowired
    protected SprintService sprintService;
    
    @Autowired
    protected UserService userService;

    /**
     * Converts a UserStoryDTO to a UserStory entity.
     * Ignores relationships that will be set in the AfterMapping method.
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
     * Ignores null values and relationships that will be set in the AfterMapping method.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "planningSessions", ignore = true)
    public abstract void updateEntityFromDto(UserStoryDTO dto, @MappingTarget UserStory userStory);

    /**
     * After mapping, set the relationships based on the IDs in the DTO.
     */
    @AfterMapping
    protected void setRelationships(UserStoryDTO dto, @MappingTarget UserStory userStory) {
        if (dto.getProjectId() != null) {
            projectService.findById(dto.getProjectId())
                    .ifPresent(userStory::setProject);
        }
        
        if (dto.getSprintId() != null) {
            sprintService.findById(dto.getSprintId())
                    .ifPresent(userStory::setSprint);
        }
        
        if (dto.getAssignedToId() != null) {
            userService.findById(dto.getAssignedToId())
                    .ifPresent(userStory::setAssignedTo);
        }
    }
}