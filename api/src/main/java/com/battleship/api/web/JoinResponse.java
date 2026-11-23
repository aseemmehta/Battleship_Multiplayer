package com.battleship.api.web;

import java.util.UUID;

/** Keep the token: it is the only credential for this game (send it as the X-Player-Token header). */
public record JoinResponse(UUID gameId, UUID token, int slot) { }
