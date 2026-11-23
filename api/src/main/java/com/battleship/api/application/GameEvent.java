package com.battleship.api.application;

import java.util.UUID;

/** Domain event for the Observer pattern; listeners are notified after the transaction commits. */
public record GameEvent(UUID gameId, String type, String detail) { }
