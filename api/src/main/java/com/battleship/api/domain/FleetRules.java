package com.battleship.api.domain;

import java.util.BitSet;
import java.util.List;

/**
 * Strategy: which board and fleet a game is played with, and how a fleet is validated.
 * Swap the implementation (see config/GameConfig) to change the rules without touching the game logic.
 */
public interface FleetRules {

    int boardSize();

    /** Ship sizes every player has to place, e.g. [3, 4, 5]. */
    List<Integer> shipSizes();

    default int totalShipCells() {
        int total = 0;
        for (int size : shipSizes()) {
            total += size;
        }
        return total;
    }

    /**
     * Checks size set, bounds and overlap. Overlap is a single BitSet test per cell,
     * so the whole check is O(total ship cells).
     */
    default void validate(List<Placement> placements) {
        List<Integer> expected = shipSizes();
        if (placements == null || placements.size() != expected.size()) {
            throw invalid("Fleet must contain exactly " + expected.size() + " ships of sizes " + expected);
        }
        List<Integer> got = placements.stream().map(p -> p == null ? -1 : p.size()).sorted().toList();
        List<Integer> want = expected.stream().sorted().toList();
        if (!got.equals(want)) {
            throw invalid("Fleet must have ship sizes " + want + " but got " + got);
        }
        int boardSize = boardSize();
        BitSet occupied = new BitSet(boardSize * boardSize);
        for (Placement p : placements) {
            if (p.orientation() == null) {
                throw invalid("Every ship needs an orientation (HORIZONTAL or VERTICAL)");
            }
            int endRow = p.row() + (p.size() - 1) * p.orientation().dRow();
            int endCol = p.col() + (p.size() - 1) * p.orientation().dCol();
            if (p.row() < 0 || p.col() < 0 || endRow >= boardSize || endCol >= boardSize) {
                throw invalid("Ship of size " + p.size() + " at (" + p.row() + "," + p.col() + ") leaves the board");
            }
            for (int k = 0; k < p.size(); k++) {
                int cell = (p.row() + k * p.orientation().dRow()) * boardSize
                        + p.col() + k * p.orientation().dCol();
                if (occupied.get(cell)) {
                    throw invalid("Ships overlap at (" + cell / boardSize + "," + cell % boardSize + ")");
                }
                occupied.set(cell);
            }
        }
    }

    private static DomainException invalid(String message) {
        return new DomainException(DomainException.Code.INVALID_PLACEMENT, message);
    }
}
