package com.battleship.api.domain;

import java.time.Instant;
import java.util.List;

/** Both players are paired and place their fleets (in any order). */
final class PlacingPhase implements GamePhase {

    @Override
    public GameStatus status() { return GameStatus.PLACING; }

    @Override
    public void placeFleet(Game game, Player player, List<Placement> placements, FleetRules rules, Instant now) {
        if (player.isFleetPlaced()) {
            throw new DomainException(DomainException.Code.ILLEGAL_STATE, "Your fleet is already placed");
        }
        rules.validate(placements);
        player.deployFleet(placements);
        game.touch(now);
        if (game.allFleetsPlaced()) {
            game.beginBattle(now);
        }
    }

    @Override
    public void expire(Game game, Instant now) {
        game.finish(null, EndReason.ABANDONED, now);
    }
}
