package com.battleship.api.application;

import com.battleship.api.domain.EndReason;
import com.battleship.api.domain.GameStatus;
import com.battleship.api.domain.ShotResult;
import java.util.List;
import java.util.UUID;

/**
 * Per-player read model returned by the API. It never contains the opponent's unsunk ships:
 * {@code enemyBoard} only has '.', 'H', 'M' and 'X'.
 * Boards are flat, row-major strings of boardSize*boardSize chars: '.' water/unknown, 'S' own ship,
 * 'H' hit, 'M' miss, 'X' sunk ship.
 */
public record GameView(
        UUID gameId,
        long version,
        GameStatus status,
        int boardSize,
        List<Integer> fleet,
        int yourSlot,
        String yourName,
        String opponentName,
        boolean fleetPlaced,
        boolean opponentFleetPlaced,
        boolean yourTurn,
        String result,
        EndReason endReason,
        String ownBoard,
        String enemyBoard,
        int yourHits,
        int enemyHits,
        int totalShipCells,
        LastShot lastShot) {

    public record LastShot(int bySlot, int row, int col, ShotResult result) { }
}
