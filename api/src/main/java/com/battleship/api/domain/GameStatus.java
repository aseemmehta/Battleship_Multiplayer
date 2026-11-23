package com.battleship.api.domain;

/** Lifecycle of a game. Each value has a matching GamePhase (State pattern). */
public enum GameStatus { WAITING, PLACING, IN_PROGRESS, FINISHED }
