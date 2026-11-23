package com.battleship.api.domain;

public record ShotOutcome(int shooterSlot, int row, int col, ShotResult result, boolean gameOver) { }
