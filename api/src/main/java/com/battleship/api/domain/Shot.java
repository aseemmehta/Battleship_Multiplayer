package com.battleship.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** A shot fired by {@code shooterSlot} at flat board index {@code cell}. The DB forbids shooting a cell twice. */
@Entity
@Table(name = "shots")
public class Shot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "shooter_slot", nullable = false)
    private int shooterSlot;

    @Column(name = "cell", nullable = false)
    private int cell;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 10)
    private ShotResult result;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt;

    protected Shot() { }

    Shot(Game game, int shooterSlot, int cell, ShotResult result, Instant firedAt) {
        this.game = game;
        this.shooterSlot = shooterSlot;
        this.cell = cell;
        this.result = result;
        this.firedAt = firedAt;
    }

    public Long getId() { return id; }
    public int getShooterSlot() { return shooterSlot; }
    public int getCell() { return cell; }
    public ShotResult getResult() { return result; }
    public Instant getFiredAt() { return firedAt; }
}
