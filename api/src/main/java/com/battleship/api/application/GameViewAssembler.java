package com.battleship.api.application;

import com.battleship.api.domain.Board;
import com.battleship.api.domain.FleetRules;
import com.battleship.api.domain.Game;
import com.battleship.api.domain.GameStatus;
import com.battleship.api.domain.Player;
import com.battleship.api.domain.Shot;
import org.springframework.stereotype.Component;

/** Assembler: turns the domain model into the player-specific {@link GameView}. */
@Component
public class GameViewAssembler {

    private final FleetRules rules;

    public GameViewAssembler(FleetRules rules) {
        this.rules = rules;
    }

    public GameView assemble(Game game, Player me) {
        int size = rules.boardSize();
        Player opponent = game.opponentOf(me);
        int opponentSlot = opponent == null ? -1 : opponent.getSlot();

        String ownBoard = Board.forTarget(size, me.getShips(), game.getShots(), opponentSlot).render(true);
        String enemyBoard = opponent == null
                ? Board.blank(size)
                : Board.forTarget(size, opponent.getShips(), game.getShots(), me.getSlot()).render(false);

        boolean yourTurn = game.getStatus() == GameStatus.IN_PROGRESS
                && game.getCurrentTurnSlot() != null
                && game.getCurrentTurnSlot() == me.getSlot();

        String result = null;
        if (game.getStatus() == GameStatus.FINISHED) {
            if (game.getWinnerSlot() == null) {
                result = "ENDED";
            } else {
                result = game.getWinnerSlot() == me.getSlot() ? "WON" : "LOST";
            }
        }

        Shot last = game.lastShot();
        GameView.LastShot lastShot = last == null ? null : new GameView.LastShot(
                last.getShooterSlot(), last.getCell() / size, last.getCell() % size, last.getResult());

        return new GameView(
                game.getId(),
                game.getVersion(),
                game.getStatus(),
                size,
                rules.shipSizes(),
                me.getSlot(),
                me.getName(),
                opponent == null ? null : opponent.getName(),
                me.isFleetPlaced(),
                opponent != null && opponent.isFleetPlaced(),
                yourTurn,
                result,
                game.getEndReason(),
                ownBoard,
                enemyBoard,
                game.hitsBy(me.getSlot()),
                opponent == null ? 0 : game.hitsBy(opponent.getSlot()),
                rules.totalShipCells(),
                lastShot);
    }
}
