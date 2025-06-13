package it.zenflow.dto;

import it.zenflow.model.project.enums.EstimationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanningPokerSessionCommand {

    @NotBlank(message = "{validation.planningpokersession.name.required}")
    @Size(min = 3, max = 100, message = "{validation.planningpokersession.name.size}")
    private String name;

    @NotNull(message = "{validation.planningpokersession.project.required}")
    private Long projectId;

    @NotNull(message = "{validation.planningpokersession.userstory.required}")
    private Long userStoryId;

    @NotEmpty(message = "{validation.planningpokersession.participants.required}")
    private List<Long> participantIds = new ArrayList<>();

    @NotNull(message = "{validation.planningpokersession.estimationtype.required}")
    private EstimationType estimationType = EstimationType.STORY_POINTS;
}
