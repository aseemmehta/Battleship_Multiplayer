package com.battleship.api.web;

import jakarta.validation.constraints.NotNull;

public record FireRequest(@NotNull Integer row, @NotNull Integer col) { }
