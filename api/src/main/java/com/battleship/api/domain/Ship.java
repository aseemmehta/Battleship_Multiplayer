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

/**
 * One ship. It keeps its own hit counter, so "is it sunk?" is O(1)
 * (the old String[][] grid needed a scan of the whole board for that).
 */
@Entity
@Table(name = "ships")
public class Ship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "size", nullable = false)
    private int size;

    @Column(name = "origin_row", nullable = false)
    private int originRow;

    @Column(name = "origin_col", nullable = false)
    private int originCol;

    @Enumerated(EnumType.STRING)
    @Column(name = "orientation", nullable = false, length = 10)
    private Orientation orientation;

    @Column(name = "hits", nullable = false)
    private int hits;

    protected Ship() { }

    Ship(Player player, Placement placement) {
        this.player = player;
        this.size = placement.size();
        this.originRow = placement.row();
        this.originCol = placement.col();
        this.orientation = placement.orientation();
    }

    /** Flat board index of the k-th cell of this ship (row * boardSize + col). */
    public int cellIndex(int k, int boardSize) {
        return (originRow + k * orientation.dRow()) * boardSize + originCol + k * orientation.dCol();
    }

    void registerHit() {
        if (hits < size) {
            hits++;
        }
    }

    public boolean isSunk() { return hits >= size; }

    public int remaining() { return size - hits; }

    public Long getId() { return id; }
    public int getSize() { return size; }
    public int getOriginRow() { return originRow; }
    public int getOriginCol() { return originCol; }
    public Orientation getOrientation() { return orientation; }
    public int getHits() { return hits; }
}
