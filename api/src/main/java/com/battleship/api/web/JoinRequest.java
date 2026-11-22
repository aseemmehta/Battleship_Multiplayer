package com.battleship.api.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRequest(@NotBlank @Size(max = 30) String name) { }
