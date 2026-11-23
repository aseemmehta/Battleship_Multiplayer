package com.battleship.api.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.battleship.api.application.GameView;
import com.battleship.api.application.GameViewAssembler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure domain tests: no Spring, no database. */
class GameFlowTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private final FleetRules rules = new ClassicFleetRules();

    private static final List<Placement> FLEET_A = List.of(
            new Placement(3, 0, 0, Orientation.HORIZONTAL),
            new Placement(4, 1, 0, Orientation.HORIZONTAL),
            new Placement(5, 2, 0, Orientation.HORIZONTAL));
    private static final List<Placement> FLEET_B = List.of(
            new Placement(3, 5, 5, Orientation.VERTICAL),
            new Placement(4, 0, 9, Orientation.VERTICAL),
            new Placement(5, 9, 0, Orientation.HORIZONTAL));

    private Game startedGame() {
        Game game = Game.open("Ann", T0);
        game.join("Bob", T0);
        game.placeFleet(game.getPlayers().get(0), FLEET_A, rules, T0);
        game.placeFleet(game.getPlayers().get(1), FLEET_B, rules, T0);
        return game;
    }

    private static DomainException.Code codeOf(Runnable action) {
        return assertThrows(DomainException.class, action::run).getCode();
    }

    @Test
    void stateMachineHappyPath() {
        Game game = Game.open("Ann", T0);
        assertEquals(GameStatus.WAITING, game.getStatus());
        game.join("Bob", T0);
        assertEquals(GameStatus.PLACING, game.getStatus());
        game.placeFleet(game.getPlayers().get(0), FLEET_A, rules, T0);
        assertEquals(GameStatus.PLACING, game.getStatus());
        game.placeFleet(game.getPlayers().get(1), FLEET_B, rules, T0);
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(1, game.getCurrentTurnSlot());
    }

    @Test
    void commandsAreRejectedInTheWrongState() {
        Game game = Game.open("Ann", T0);
        Player ann = game.getPlayers().get(0);
        assertEquals(DomainException.Code.ILLEGAL_STATE, codeOf(() -> game.fire(ann, 0, 0, rules, T0)));
        assertEquals(DomainException.Code.ILLEGAL_STATE, codeOf(() -> game.placeFleet(ann, FLEET_A, rules, T0)));
        game.join("Bob", T0);
        assertEquals(DomainException.Code.ILLEGAL_STATE, codeOf(() -> game.join("Eve", T0)));
        assertEquals(DomainException.Code.ILLEGAL_STATE, codeOf(() -> game.fire(ann, 0, 0, rules, T0)));
        game.placeFleet(ann, FLEET_A, rules, T0);
        assertEquals(DomainException.Code.ILLEGAL_STATE, codeOf(() -> game.placeFleet(ann, FLEET_A, rules, T0)));
    }

    @Test
    void fleetValidation() {
        Game game = Game.open("Ann", T0);
        game.join("Bob", T0);
        Player ann = game.getPlayers().get(0);
        List<Placement> overlap = List.of(
                new Placement(3, 0, 0, Orientation.HORIZONTAL),
                new Placement(4, 0, 2, Orientation.HORIZONTAL),
                new Placement(5, 5, 0, Orientation.HORIZONTAL));
        List<Placement> offBoard = List.of(
                new Placement(3, 0, 0, Orientation.HORIZONTAL),
                new Placement(4, 1, 0, Orientation.HORIZONTAL),
                new Placement(5, 2, 7, Orientation.HORIZONTAL));
        List<Placement> wrongSizes = List.of(
                new Placement(3, 0, 0, Orientation.HORIZONTAL),
                new Placement(3, 1, 0, Orientation.HORIZONTAL),
                new Placement(5, 2, 0, Orientation.HORIZONTAL));
        List<Placement> tooFew = List.of(new Placement(3, 0, 0, Orientation.HORIZONTAL));
        for (List<Placement> bad : List.of(overlap, offBoard, wrongSizes, tooFew)) {
            assertEquals(DomainException.Code.INVALID_PLACEMENT,
                    codeOf(() -> game.placeFleet(ann, bad, rules, T0)));
        }
        assertFalse(ann.isFleetPlaced());
    }

    @Test
    void turnsAndShotRules() {
        Game game = startedGame();
        Player ann = game.getPlayers().get(0);
        Player bob = game.getPlayers().get(1);

        assertEquals(DomainException.Code.NOT_YOUR_TURN, codeOf(() -> game.fire(bob, 0, 0, rules, T0)));
        assertEquals(DomainException.Code.INVALID_SHOT, codeOf(() -> game.fire(ann, 10, 0, rules, T0)));
        assertEquals(DomainException.Code.INVALID_SHOT, codeOf(() -> game.fire(ann, 0, -1, rules, T0)));

        ShotOutcome miss = game.fire(ann, 0, 0, rules, T0);        // water on Bob's board
        assertEquals(ShotResult.MISS, miss.result());
        assertEquals(2, game.getCurrentTurnSlot());
        game.fire(bob, 9, 9, rules, T0);                           // pass the turn back
        assertEquals(DomainException.Code.ALREADY_SHOT, codeOf(() -> game.fire(ann, 0, 0, rules, T0)));
    }

    @Test
    void sinkingShipsAndWinning() {
        Game game = startedGame();
        Player ann = game.getPlayers().get(0);
        Player bob = game.getPlayers().get(1);

        // Bob's ships: (5,5)-(7,5) size 3, (0,9)-(3,9) size 4, (9,0)-(9,4) size 5
        List<int[]> bobCells = List.of(
                new int[]{5, 5}, new int[]{6, 5}, new int[]{7, 5},
                new int[]{0, 9}, new int[]{1, 9}, new int[]{2, 9}, new int[]{3, 9},
                new int[]{9, 0}, new int[]{9, 1}, new int[]{9, 2}, new int[]{9, 3}, new int[]{9, 4});
        for (int i = 0; i < bobCells.size(); i++) {
            int[] cell = bobCells.get(i);
            ShotOutcome outcome = game.fire(ann, cell[0], cell[1], rules, T0);
            boolean last = i == bobCells.size() - 1;
            boolean endsShip = i == 2 || i == 6 || i == 11;
            assertEquals(endsShip ? ShotResult.SUNK : ShotResult.HIT, outcome.result());
            assertEquals(last, outcome.gameOver());
            if (!last) {
                // Bob shoots water on Ann's board (her ships are in rows 0-2): row 4, then (3,0)
                int row = i < 10 ? 4 : 3;
                int col = i < 10 ? i : 0;
                assertEquals(ShotResult.MISS, game.fire(bob, row, col, rules, T0).result());
            }
        }
        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(1, game.getWinnerSlot());
        assertEquals(EndReason.ALL_SHIPS_SUNK, game.getEndReason());
        assertEquals(12, game.hitsBy(1));
        assertEquals(DomainException.Code.ILLEGAL_STATE, codeOf(() -> game.fire(bob, 0, 0, rules, T0)));
    }

    @Test
    void expiry() {
        Game waiting = Game.open("Ann", T0);
        waiting.expire(T0);
        assertEquals(GameStatus.FINISHED, waiting.getStatus());
        assertEquals(EndReason.ABANDONED, waiting.getEndReason());
        assertNull(waiting.getWinnerSlot());

        Game running = startedGame();               // slot 1 is to move
        running.expire(T0);
        assertEquals(GameStatus.FINISHED, running.getStatus());
        assertEquals(EndReason.TIMEOUT, running.getEndReason());
        assertEquals(2, running.getWinnerSlot());   // the player who did not move loses
    }

    @Test
    void viewsNeverRevealTheOpponentsShips() {
        Game game = startedGame();
        Player ann = game.getPlayers().get(0);
        Player bob = game.getPlayers().get(1);
        GameViewAssembler assembler = new GameViewAssembler(rules);

        GameView annView = assembler.assemble(game, ann);
        assertEquals(100, annView.ownBoard().length());
        assertEquals(12, annView.ownBoard().chars().filter(c -> c == 'S').count());
        assertEquals(-1, annView.enemyBoard().indexOf('S'));
        assertTrue(annView.yourTurn());
        assertFalse(assembler.assemble(game, bob).yourTurn());

        game.fire(ann, 5, 5, rules, T0);            // hit Bob's size-3 ship
        GameView after = assembler.assemble(game, ann);
        assertEquals('H', after.enemyBoard().charAt(55));
        assertEquals(1, after.yourHits());
        assertEquals('H', assembler.assemble(game, bob).ownBoard().charAt(55));
        assertEquals(new GameView.LastShot(1, 5, 5, ShotResult.HIT), after.lastShot());
    }
}
