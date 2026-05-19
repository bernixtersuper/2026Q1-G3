package com.menudigital.application.analytics.dto;

import com.menudigital.domain.analytics.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record RecordEventCommand(
    @NotNull EventType eventType,
    String itemId,
    String sectionId,
    @NotBlank String sessionId,
    Map<String, String> metadata
) {}
