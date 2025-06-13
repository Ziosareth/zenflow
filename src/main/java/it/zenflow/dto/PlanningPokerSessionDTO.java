package it.zenflow.dto;

import it.zenflow.model.project.enums.EstimationType;
import it.zenflow.model.project.enums.SessionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanningPokerSessionDTO {

    private Long id;

    @NotBlank(message = "{validation.planningpokersession.name.required}")
    @Size(min = 3, max = 100, message = "{validation.planningpokersession.name.size}")
    private String name;

    @NotNull(message = "{validation.planningpokersession.status.required}")
    private SessionStatus status = SessionStatus.CREATED;

    @NotNull(message = "{validation.planningpokersession.project.required}")
    private Long projectId;
    
    private String projectName;

    @NotNull(message = "{validation.planningpokersession.facilitator.required}")
    private Long facilitatorId;
    
    private String facilitatorName;
    
    private EstimationType estimationType = EstimationType.STORY_POINTS;
    
    private Set<Long> participantIds = new HashSet<>();
    
    private List<Long> userStoryIds = new ArrayList<>();
    
    // These fields are used for display purposes only
    private Set<UserDTO> participants = new HashSet<>();
    
    private List<UserStoryDTO> userStories = new ArrayList<>();
    
    private List<EstimationVoteDTO> votes = new ArrayList<>();
}