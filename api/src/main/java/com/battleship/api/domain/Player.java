package com.battleship.api.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /** 1 = created the game (shoots first), 2 = joined it. */
    @Column(name = "slot", nullable = false)
    private int slot;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    /** Secret the client sends as X-Player-Token; it is what authorises moves for this player. */
    @Column(name = "token", nullable = false, updatable = false)
    private UUID token;

    @Column(name = "fleet_placed", nullable = false)
    private boolean fleetPlaced;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Ship> ships = new HashSet<>();

    protected Player() { }

    Player(Game game, int slot, String name) {
        this.game = game;
        this.slot = slot;
        this.name = normalise(name, slot);
        this.token = UUID.randomUUID();
    }

    private static String normalise(String raw, int slot) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return "Player " + slot;
        }
        return trimmed.length() > 30 ? trimmed.substring(0, 30) : trimmed;
    }

    void deployFleet(List<Placement> placements) {
        for (Placement placement : placements) {
            ships.add(new Ship(this, placement));
        }
        fleetPlaced = true;
    }

    public boolean allShipsSunk() {
        for (Ship ship : ships) {
            if (!ship.isSunk()) {
                return false;
            }
        }
        return true;
    }

    public UUID getId() { return id; }
    public int getSlot() { return slot; }
    public String getName() { return name; }
    public UUID getToken() { return token; }
    public boolean isFleetPlaced() { return fleetPlaced; }
    public Set<Ship> getShips() { return Collections.unmodifiableSet(ships); }
}
