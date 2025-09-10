package it.zenflow.controller.api;

import it.zenflow.metrics.BurndownService;
import it.zenflow.metrics.dto.BurndownResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics/burndown")
@RequiredArgsConstructor
class BurndownController {

    private final BurndownService burndownService;

    @GetMapping("/sprint/{sprintId}")
    ResponseEntity<BurndownResponse> sprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(burndownService.getSprintBurndown(sprintId));
    }
}
