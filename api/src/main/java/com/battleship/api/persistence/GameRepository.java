package com.battleship.api.persistence;

import com.battleship.api.domain.Game;
import com.battleship.api.domain.GameStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/** Repository pattern over Postgres via JPA. */
public interface GameRepository extends JpaRepository<Game, UUID> {

    /** Row lock for the duration of the transaction; serialises moves within one game. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Game g where g.id = :id")
    Optional<Game> lockById(@Param("id") UUID id);

    /** Oldest waiting game that nobody else is currently joining (lock timeout -2 = SKIP LOCKED). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select g from Game g where g.status = com.battleship.api.domain.GameStatus.WAITING order by g.createdAt asc")
    List<Game> lockOldestWaiting(Pageable pageable);

    @Query("select g.id from Game g where g.status = :status and g.updatedAt < :cutoff")
    List<UUID> findIdsByStatusIdleSince(@Param("status") GameStatus status, @Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from Game g where g.status = com.battleship.api.domain.GameStatus.FINISHED and g.updatedAt < :cutoff")
    int deleteFinishedBefore(@Param("cutoff") Instant cutoff);
}
