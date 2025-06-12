package it.zenflow.model.project.enums;

/**
 * Enum representing the type of estimation used for a user story.
 */
public enum EstimationType {
    /**
     * Manual story point estimation.
     */
    STORY_POINTS,
    
    /**
     * PERT (Program Evaluation and Review Technique) estimation.
     * Story points are calculated from optimistic, most likely, and pessimistic estimates.
     */
    PERT
}