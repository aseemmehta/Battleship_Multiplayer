package com.battleship.api.domain;

import java.time.Instant;

/** One player is waiting for an opponent. */
final class WaitingPhase implements GamePhase {

    @Override
    public GameStatus status() { return GameStatus.WAITING; }

    @Override
    public Player join(Game game, String name, Instant now) {
        Player player = game.addPlayer(name, now);
        game.setStatus(GameStatus.PLACING, now);
        return player;
    }

    @Override
    public void expire(Game game, Instant now) {
        game.finish(null, EndReason.ABANDONED, now);
    }
}
