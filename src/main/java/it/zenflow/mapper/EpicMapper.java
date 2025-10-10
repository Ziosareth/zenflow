package it.zenflow.mapper;

import it.zenflow.dto.EpicDTO;
import it.zenflow.model.project.Epic;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EpicMapper {

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "milestones", ignore = true)
    Epic toEntity(EpicDTO dto);

    @Mapping(target = "projectId", source = "project.id")
    EpicDTO toDto(Epic epic);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "milestones", ignore = true)
    void updateEntityFromDto(EpicDTO dto, @MappingTarget Epic epic);
}
