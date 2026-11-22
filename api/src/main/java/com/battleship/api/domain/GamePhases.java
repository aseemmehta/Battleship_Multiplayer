package com.battleship.api.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Registry of the phase objects. EnumMap = array lookup by ordinal. */
final class GamePhases {

    private static final Map<GameStatus, GamePhase> PHASES = new EnumMap<>(GameStatus.class);

    static {
        for (GamePhase phase : List.of(new WaitingPhase(), new PlacingPhase(), new InProgressPhase(), new FinishedPhase())) {
            PHASES.put(phase.status(), phase);
        }
    }

    private GamePhases() { }

    static GamePhase of(GameStatus status) {
        return PHASES.get(status);
    }
}
