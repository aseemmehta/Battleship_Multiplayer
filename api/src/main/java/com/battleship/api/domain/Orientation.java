package com.battleship.api.domain;

public enum Orientation {
    HORIZONTAL(0, 1),
    VERTICAL(1, 0);

    private final int dRow;
    private final int dCol;

    Orientation(int dRow, int dCol) {
        this.dRow = dRow;
        this.dCol = dCol;
    }

    public int dRow() { return dRow; }
    public int dCol() { return dCol; }
}
