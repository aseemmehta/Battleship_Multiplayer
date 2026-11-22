package com.battleship.api.domain;

/** Terminal state: every command is rejected by the interface defaults. */
final class FinishedPhase implements GamePhase {

    @Override
    public GameStatus status() { return GameStatus.FINISHED; }
}
