package com.battleship.api.domain;

import java.util.List;

/** Same game as the original code: 10x10 board, three ships of size 3, 4 and 5. */
public class ClassicFleetRules implements FleetRules {

    private static final List<Integer> SIZES = List.of(3, 4, 5);

    @Override
    public int boardSize() { return 10; }

    @Override
    public List<Integer> shipSizes() { return SIZES; }
}
