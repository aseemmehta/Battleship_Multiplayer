package com.battleship.api.web;

import com.battleship.api.domain.Placement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlaceFleetRequest(@NotNull @Size(min = 1, max = 10) List<Placement> ships) { }
