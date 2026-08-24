package com.navvis.locator.adapter.in.web.location.dto;

import jakarta.validation.constraints.NotNull;

public record LocateRequest(
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double z
) {}