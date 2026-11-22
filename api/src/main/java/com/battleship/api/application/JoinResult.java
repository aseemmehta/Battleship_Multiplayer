package com.battleship.api.application;

import java.util.UUID;

public record JoinResult(UUID gameId, UUID token, int slot) { }
