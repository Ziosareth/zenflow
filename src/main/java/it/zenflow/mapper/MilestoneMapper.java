package it.zenflow.mapper;

import it.zenflow.dto.MilestoneDTO;
import it.zenflow.model.project.Milestone;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MilestoneMapper {

    @Mapping(target = "project", ignore = true)
    @Mapping(target = "epics", ignore = true)
    Milestone toEntity(MilestoneDTO dto);

    @Mapping(target = "projectId", source = "project.id")
    MilestoneDTO toDto(Milestone milestone);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "epics", ignore = true)
    void updateEntityFromDto(MilestoneDTO dto, @MappingTarget Milestone milestone);
}
