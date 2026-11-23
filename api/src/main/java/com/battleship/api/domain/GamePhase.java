package com.battleship.api.domain;

import java.time.Instant;
import java.util.List;

/**
 * State pattern. One implementation per {@link GameStatus}; anything a state does not override is rejected
 * with ILLEGAL_STATE. This is the state machine from the original Client/Game code, made explicit:
 *
 *   WAITING --join--> PLACING --both fleets placed--> IN_PROGRESS --last ship sunk / timeout--> FINISHED
 */
interface GamePhase {

    GameStatus status();

    default Player join(Game game, String name, Instant now) {
        throw illegal("join");
    }

    default void placeFleet(Game game, Player player, List<Placement> placements, FleetRules rules, Instant now) {
        throw illegal("place a fleet");
    }

    default ShotOutcome fire(Game game, Player shooter, int row, int col, FleetRules rules, Instant now) {
        throw illegal("fire");
    }

    /** What happens when the game has been idle for too long. */
    default void expire(Game game, Instant now) { }

    private DomainException illegal(String action) {
        return new DomainException(DomainException.Code.ILLEGAL_STATE,
                "Cannot " + action + " while the game is " + status());
    }
}
