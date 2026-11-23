package com.battleship.api.domain;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;

/**
 * Read model of one player's board, built from the persisted ships and shots.
 *
 * Data structures (all O(1) per cell, 100 cells):
 *  - Ship[]  : cell index -> ship (null = water), replaces the old String[][] + String.equals checks
 *  - BitSet  : cells already shot at (duplicate-shot test is one bit read)
 * Cells are addressed by a flat index = row * size + col.
 */
public final class Board {

    private final int size;
    private final Ship[] shipAt;
    private final BitSet shots = new BitSet();

    private Board(int size) {
        this.size = size;
        this.shipAt = new Ship[size * size];
    }

    /**
     * @param shooterSlot only shots fired by this slot count as shots at this board; -1 = none
     */
    public static Board forTarget(int size, Collection<Ship> ships, Collection<Shot> allShots, int shooterSlot) {
        Board board = new Board(size);
        for (Ship ship : ships) {
            for (int k = 0; k < ship.getSize(); k++) {
                board.shipAt[ship.cellIndex(k, size)] = ship;
            }
        }
        for (Shot shot : allShots) {
            if (shot.getShooterSlot() == shooterSlot) {
                board.shots.set(shot.getCell());
            }
        }
        return board;
    }

    /** An empty, unshot board of '.' (used before an opponent exists). */
    public static String blank(int size) {
        char[] cells = new char[size * size];
        Arrays.fill(cells, '.');
        return new String(cells);
    }

    public int index(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size) {
            throw new DomainException(DomainException.Code.INVALID_SHOT,
                    "Cell (" + row + "," + col + ") is outside the " + size + "x" + size + " board");
        }
        return row * size + col;
    }

    public boolean alreadyShot(int cell) { return shots.get(cell); }

    public Ship shipAt(int cell) { return shipAt[cell]; }

    /**
     * One char per cell, row-major: '.' water/unknown, 'S' ship (owner only), 'H' hit, 'M' miss, 'X' sunk ship.
     * A single 100-char string replaces the old per-cell System.out.print calls and is what goes over the wire.
     */
    public String render(boolean showShips) {
        char[] out = new char[shipAt.length];
        Arrays.fill(out, '.');
        if (showShips) {
            for (int i = 0; i < shipAt.length; i++) {
                if (shipAt[i] != null) {
                    out[i] = 'S';
                }
            }
        }
        for (int i = shots.nextSetBit(0); i >= 0; i = shots.nextSetBit(i + 1)) {
            Ship ship = shipAt[i];
            out[i] = ship == null ? 'M' : (ship.isSunk() ? 'X' : 'H');
        }
        return new String(out);
    }
}
