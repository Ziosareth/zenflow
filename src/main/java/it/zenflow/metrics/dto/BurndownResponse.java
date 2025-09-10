package it.zenflow.metrics.dto;

import java.time.LocalDate;
import java.util.List;

public record BurndownResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<LocalDate> labels,
        List<Integer> ideal,
        List<Integer> remaining,
        List<Integer> scope,
        String unit
) {}
