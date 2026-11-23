package com.battleship.api.domain;

import java.time.Instant;

/** Players take turns firing. */
final class InProgressPhase implements GamePhase {

    @Override
    public GameStatus status() { return GameStatus.IN_PROGRESS; }

    @Override
    public ShotOutcome fire(Game game, Player shooter, int row, int col, FleetRules rules, Instant now) {
        Integer turn = game.getCurrentTurnSlot();
        if (turn == null || turn.intValue() != shooter.getSlot()) {
            throw new DomainException(DomainException.Code.NOT_YOUR_TURN, "It is not your turn");
        }
        Player target = game.opponentOf(shooter);
        Board board = Board.forTarget(rules.boardSize(), target.getShips(), game.getShots(), shooter.getSlot());
        int cell = board.index(row, col);
        if (board.alreadyShot(cell)) {
            throw new DomainException(DomainException.Code.ALREADY_SHOT,
                    "You already fired at (" + row + "," + col + ")");
        }

        Ship ship = board.shipAt(cell);
        ShotResult result;
        if (ship == null) {
            result = ShotResult.MISS;
        } else {
            ship.registerHit();
            result = ship.isSunk() ? ShotResult.SUNK : ShotResult.HIT;
        }
        game.recordShot(shooter.getSlot(), cell, result, now);

        boolean gameOver = result != ShotResult.MISS && target.allShipsSunk();
        if (gameOver) {
            game.finish(shooter.getSlot(), EndReason.ALL_SHIPS_SUNK, now);
        } else {
            game.passTurnTo(target.getSlot(), now);
        }
        return new ShotOutcome(shooter.getSlot(), row, col, result, gameOver);
    }

    /** The player whose turn it is took too long and forfeits. */
    @Override
    public void expire(Game game, Instant now) {
        Integer turn = game.getCurrentTurnSlot();
        Integer winner = turn == null ? null : Integer.valueOf(3 - turn.intValue());
        game.finish(winner, EndReason.TIMEOUT, now);
    }
}
