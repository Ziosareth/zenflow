package it.zenflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimationVoteDTO {
    
    private Long id;
    
    @NotNull(message = "{validation.estimationvote.session.required}")
    private Long sessionId;
    
    @NotNull(message = "{validation.estimationvote.userstory.required}")
    private Long userStoryId;
    
    private String userStoryTitle;
    
    @NotNull(message = "{validation.estimationvote.voter.required}")
    private Long voterId;
    
    private String voterName;
    
    private Integer storyPoints;
    
    private String reasoning;
}