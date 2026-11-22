package com.battleship.api.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root. The persisted state is: status, whose turn it is, both players with their ships, and the
 * list of shots. The boards are derived from ships + shots (see {@link Board}).
 *
 * Behaviour is delegated to the current {@link GamePhase} (State pattern), so every rule
 * "what may happen in which state" lives in exactly one place.
 */
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameStatus status;

    @Column(name = "current_turn_slot")
    private Integer currentTurnSlot;

    @Column(name = "winner_slot")
    private Integer winnerSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 20)
    private EndReason endReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic-locking counter; it also doubles as the ETag the clients poll with. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("slot ASC")
    private List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Shot> shots = new ArrayList<>();

    protected Game() { }

    private Game(Instant now) {
        this.status = GameStatus.WAITING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Factory method: a new game that waits for an opponent. The creator becomes slot 1. */
    public static Game open(String creatorName, Instant now) {
        Game game = new Game(now);
        game.addPlayer(creatorName, now);
        return game;
    }

    // ---- behaviour, delegated to the current state ------------------------------------------

    public Player join(String name, Instant now) {
        return phase().join(this, name, now);
    }

    public void placeFleet(Player player, List<Placement> placements, FleetRules rules, Instant now) {
        phase().placeFleet(this, player, placements, rules, now);
    }

    public ShotOutcome fire(Player shooter, int row, int col, FleetRules rules, Instant now) {
        return phase().fire(this, shooter, row, col, rules, now);
    }

    /** Called by the janitor when nobody has acted for too long. */
    public void expire(Instant now) {
        phase().expire(this, now);
    }

    private GamePhase phase() {
        return GamePhases.of(status);
    }

    // ---- queries ----------------------------------------------------------------------------

    public Player requirePlayer(UUID token) {
        for (Player player : players) {
            if (player.getToken().equals(token)) {
                return player;
            }
        }
        throw new DomainException(DomainException.Code.FORBIDDEN, "Unknown player token for this game");
    }

    /** The other player, or null while the game is still waiting for one. */
    public Player opponentOf(Player player) {
        for (Player other : players) {
            if (other.getSlot() != player.getSlot()) {
                return other;
            }
        }
        return null;
    }

    public int hitsBy(int slot) {
        int hits = 0;
        for (Shot shot : shots) {
            if (shot.getShooterSlot() == slot && shot.getResult() != ShotResult.MISS) {
                hits++;
            }
        }
        return hits;
    }

    public Shot lastShot() {
        return shots.isEmpty() ? null : shots.get(shots.size() - 1);
    }

    // ---- package-private mutators, used by the phases only -----------------------------------

    Player addPlayer(String name, Instant now) {
        Player player = new Player(this, players.size() + 1, name);
        players.add(player);
        touch(now);
        return player;
    }

    void setStatus(GameStatus newStatus, Instant now) {
        this.status = newStatus;
        touch(now);
    }

    boolean allFleetsPlaced() {
        if (players.size() != 2) {
            return false;
        }
        for (Player player : players) {
            if (!player.isFleetPlaced()) {
                return false;
            }
        }
        return true;
    }

    /** Both fleets are ready: slot 1 shoots first (as in the original game). */
    void beginBattle(Instant now) {
        this.status = GameStatus.IN_PROGRESS;
        this.currentTurnSlot = 1;
        touch(now);
    }

    void recordShot(int shooterSlot, int cell, ShotResult result, Instant now) {
        shots.add(new Shot(this, shooterSlot, cell, result, now));
        touch(now);
    }

    void passTurnTo(int slot, Instant now) {
        this.currentTurnSlot = slot;
        touch(now);
    }

    void finish(Integer winner, EndReason reason, Instant now) {
        this.status = GameStatus.FINISHED;
        this.currentTurnSlot = null;
        this.winnerSlot = winner;
        this.endReason = reason;
        touch(now);
    }

    void touch(Instant now) {
        this.updatedAt = now;
    }

    // ---- getters ----------------------------------------------------------------------------

    public UUID getId() { return id; }
    public GameStatus getStatus() { return status; }
    public Integer getCurrentTurnSlot() { return currentTurnSlot; }
    public Integer getWinnerSlot() { return winnerSlot; }
    public EndReason getEndReason() { return endReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version == null ? 0L : version; }
    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public List<Shot> getShots() { return Collections.unmodifiableList(shots); }
}
