package com.battleship.api.application;

import com.battleship.api.domain.DomainException;
import com.battleship.api.domain.FleetRules;
import com.battleship.api.domain.Game;
import com.battleship.api.domain.GameStatus;
import com.battleship.api.domain.Placement;
import com.battleship.api.domain.Player;
import com.battleship.api.domain.ShotOutcome;
import com.battleship.api.persistence.GameRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service (Facade): the only entry point the web layer uses. Each public method is one
 * transaction. Mutations take a row lock on the game, so two phones can never change the same game at once.
 */
@Service
public class GameService {

    private final GameRepository games;
    private final FleetRules rules;
    private final GameViewAssembler assembler;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public GameService(GameRepository games, FleetRules rules, GameViewAssembler assembler,
                       Clock clock, ApplicationEventPublisher events) {
        this.games = games;
        this.rules = rules;
        this.assembler = assembler;
        this.clock = clock;
        this.events = events;
    }

    /** Matchmaking: join the oldest waiting game, or open a new one. */
    @Transactional
    public JoinResult join(String name) {
        Instant now = clock.instant();
        // FOR UPDATE SKIP LOCKED: two simultaneous requests never grab the same waiting game
        List<Game> waiting = games.lockOldestWaiting(PageRequest.of(0, 1));
        Game game;
        Player player;
        if (waiting.isEmpty()) {
            game = games.save(Game.open(name, now));
            player = game.getPlayers().get(0);
            events.publishEvent(new GameEvent(game.getId(), "OPENED", player.getName() + " is waiting"));
        } else {
            game = waiting.get(0);
            player = game.join(name, now);
            events.publishEvent(new GameEvent(game.getId(), "PAIRED", player.getName() + " joined"));
        }
        games.flush();
        return new JoinResult(game.getId(), player.getToken(), player.getSlot());
    }

    @Transactional(readOnly = true)
    public GameView view(UUID gameId, UUID token) {
        Game game = games.findById(gameId).orElseThrow(() -> notFound(gameId));
        Player me = game.requirePlayer(token);
        return assembler.assemble(game, me);
    }

    @Transactional
    public GameView placeFleet(UUID gameId, UUID token, List<Placement> placements) {
        Game game = games.lockById(gameId).orElseThrow(() -> notFound(gameId));
        Player me = game.requirePlayer(token);
        GameStatus before = game.getStatus();
        game.placeFleet(me, placements, rules, clock.instant());
        games.flush();
        events.publishEvent(new GameEvent(gameId, "FLEET_PLACED", me.getName()
                + (before != game.getStatus() ? " (battle starts)" : "")));
        return assembler.assemble(game, me);
    }

    @Transactional
    public GameView fire(UUID gameId, UUID token, int row, int col) {
        Game game = games.lockById(gameId).orElseThrow(() -> notFound(gameId));
        Player me = game.requirePlayer(token);
        ShotOutcome outcome = game.fire(me, row, col, rules, clock.instant());
        games.flush();
        events.publishEvent(new GameEvent(gameId, outcome.gameOver() ? "GAME_OVER" : "SHOT",
                me.getName() + " fired at (" + row + "," + col + "): " + outcome.result()));
        return assembler.assemble(game, me);
    }

    /** Janitor: end games of the given status that nobody touched since {@code cutoff}. */
    @Transactional
    public int expireIdle(GameStatus status, Instant cutoff, Instant now) {
        int expired = 0;
        for (UUID id : games.findIdsByStatusIdleSince(status, cutoff)) {
            Game game = games.lockById(id).orElse(null);
            if (game != null && game.getStatus() == status && game.getUpdatedAt().isBefore(cutoff)) {
                game.expire(now);
                events.publishEvent(new GameEvent(id, "EXPIRED", status + " game timed out"));
                expired++;
            }
        }
        return expired;
    }

    /** Janitor: delete finished games (shots/ships/players go with them via ON DELETE CASCADE). */
    @Transactional
    public int purgeFinishedBefore(Instant cutoff) {
        return games.deleteFinishedBefore(cutoff);
    }

    private static DomainException notFound(UUID gameId) {
        return new DomainException(DomainException.Code.GAME_NOT_FOUND, "Game " + gameId + " not found");
    }
}
