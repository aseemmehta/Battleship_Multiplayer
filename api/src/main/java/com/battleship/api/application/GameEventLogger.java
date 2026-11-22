package com.battleship.api.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer of {@link GameEvent}. Today it only logs; a push channel (SSE/WebSocket) can subscribe the same way
 * and replace client polling without touching the game logic.
 */
@Component
public class GameEventLogger {

    private static final Logger log = LoggerFactory.getLogger(GameEventLogger.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(GameEvent event) {
        log.info("game={} {} {}", event.gameId(), event.type(), event.detail());
    }
}
