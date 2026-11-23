package com.battleship.api.domain;

/** Value object: one ship as sent by a player (origin cell + direction). */
public record Placement(int size, int row, int col, Orientation orientation) { }
