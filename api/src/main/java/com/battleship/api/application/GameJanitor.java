package com.battleship.api.application;

import com.battleship.api.domain.GameStatus;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Replaces the old 60 s busy-wait loops and the never-expiring "waiting player" slot on the server:
 * idle games are ended in the background, so no thread ever sits and waits for an opponent.
 */
@Component
public class GameJanitor {

    private static final Logger log = LoggerFactory.getLogger(GameJanitor.class);

    private final GameService service;
    private final Clock clock;
    private final long waitingSeconds;
    private final long placingSeconds;
    private final long turnSeconds;
    private final long keepFinishedSeconds;

    public GameJanitor(GameService service, Clock clock,
                       @Value("${battleship.timeouts.waiting-seconds:600}") long waitingSeconds,
                       @Value("${battleship.timeouts.placing-seconds:600}") long placingSeconds,
                       @Value("${battleship.timeouts.turn-seconds:120}") long turnSeconds,
                       @Value("${battleship.timeouts.keep-finished-seconds:86400}") long keepFinishedSeconds) {
        this.service = service;
        this.clock = clock;
        this.waitingSeconds = waitingSeconds;
        this.placingSeconds = placingSeconds;
        this.turnSeconds = turnSeconds;
        this.keepFinishedSeconds = keepFinishedSeconds;
    }

    @Scheduled(fixedDelayString = "${battleship.janitor.interval-ms:15000}")
    public void sweep() {
        Instant now = clock.instant();
        int expired = service.expireIdle(GameStatus.WAITING, now.minusSeconds(waitingSeconds), now)
                + service.expireIdle(GameStatus.PLACING, now.minusSeconds(placingSeconds), now)
                + service.expireIdle(GameStatus.IN_PROGRESS, now.minusSeconds(turnSeconds), now);
        int purged = service.purgeFinishedBefore(now.minusSeconds(keepFinishedSeconds));
        if (expired > 0 || purged > 0) {
            log.info("janitor: expired {} idle games, purged {} finished games", expired, purged);
        }
    }
}
