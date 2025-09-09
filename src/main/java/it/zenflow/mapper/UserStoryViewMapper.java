package it.zenflow.mapper;

import it.zenflow.dto.UserStoryViewDTO;
import it.zenflow.model.project.UserStory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserStoryViewMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "sprintId", source = "sprint.id")
    @Mapping(target = "assignedToUsername", source = "assignedTo.username")
    @Mapping(target = "statusName", source = "status", qualifiedByName = "enumName")
    @Mapping(target = "priorityName", source = "priority", qualifiedByName = "enumName")
    @Mapping(target = "estimationTypeName", source = "estimationType", qualifiedByName = "enumName")
    @Mapping(target = "tasksCount", expression = "java(entity.getTasks() != null ? entity.getTasks().size() : 0)")
    UserStoryViewDTO toViewDto(UserStory entity);

    @Named("enumName")
    default String enumName(Enum<?> e) {
        return e != null ? e.name() : null;
    }
}
