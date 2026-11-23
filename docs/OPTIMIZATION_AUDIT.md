# Where the original code was slow or wasteful, and what replaced it

Scope: `Client/` and `Server/` of the UDP version. Items are ordered by impact.

| # | Where | Problem | Replaced by |
|---|-------|---------|-------------|
| 1 | `Game.java`, `Listen.java`: busy-wait loops (`while(time < 60000 && status == 6){ System.out.print(""); }`, also in `Sender.play` and the `Code:04` handler) | Burns 100 % of a CPU core while waiting. The empty `print` only existed to stop the JIT from hoisting a non-volatile read, i.e. a data race hidden behind I/O. The `Code:04` handler also blocked the receive thread for up to 60 s. | No waiting threads: state is in Postgres, clients poll with ETag/304, a scheduled janitor ends idle games. |
| 2 | `Game.java`: `String[][]` board with tokens `" X"`, `"S1"`, `"HH"`, `"MM"` and `.equals` on every check | 100 object references per board, pointer chasing, string compares, magic strings. | `Ship[]` (cell → ship) and `BitSet` (shots) in `Board`; flat 100-char string on the wire. |
| 3 | Same grid: "is this ship sunk?" | Ships only exist as `"S1"` labels, so answering needs a scan of the whole board (O(n²)). | `Ship.hits` counter, `isSunk()` is O(1). |
| 4 | `Game.java`: duplicate-shot / bounds checks via exceptions (`catch(Exception){ input.next(); }`) | Exceptions as control flow; `input.next()` in the handler can itself throw. | Bounds check in `Board.index`, duplicate check = one `BitSet` bit + DB unique constraint. |
| 5 | `Sender.sendMessage` | New `DatagramSocket` (file descriptor + ephemeral port) for every message. | HTTP keep-alive through the gateway; Hikari connection pool to Postgres. |
| 6 | `Server/Listen.java`: static `p1_IP` as "the queue" | Holds one waiting player; `p1_Time` is stored but never read, so a player who left stays matched forever. Static mutable state without synchronisation. | Waiting games in Postgres: partial index on `created_at WHERE status='WAITING'`, `FOR UPDATE SKIP LOCKED`, janitor expiry. |
| 7 | `Game.PrintMatrix` | ~500 single `System.out.print` calls (each synchronised, flushed on a terminal) plus string concatenation. | One `char[]` → `String` per board (`Board.render`); UI is React. |
| 8 | `Listen` / protocol: `msg.substring(0,7)` chain + `parseInt(substring(7,8))` | Allocates substrings per message; hard-wired to single-digit coordinates. | Typed JSON (Jackson records). |
| 9 | `double t = System.currentTimeMillis()` | `long` kept in a `double` and compared in the hot loop. | No timestamps in loops; `Instant` + a `Clock` bean. |
| 10 | `Listen.receiveUDPMessage` | Sender IP rebuilt with manual masking/concatenation per packet; a new `DatagramPacket` per iteration. | Not needed (HTTP). |
| 11 | `Game.hitMissCheck` | Dead copy of `shipCheck`, never called. | Removed. |
| 12 | `Client/Dockerfile` | Unpinned `openjdk` image, `yum install iptables net-tools nodejs` that the game never uses, runs as root, needs `NET_ADMIN`, compiles with `javac *.java` at build. | Pinned multi-stage builds, JRE-only runtime, non-root users, no extra capabilities. |

Not a problem in the original: the score is an O(1) counter (no rescanning of the grid to detect a win).

## New code: things to watch

- `GET /api/games/{id}` still builds the full view on every poll; the ETag saves bandwidth, not database work.
  A cheaper "version probe" (select `version` only) or push via SSE/WebSocket would remove that.
  `GameEvent` is the hook for it.
- `Game.shots` is loaded in full for every move (at most 2 x 100 rows); fine here, revisit for much larger boards.
- Matchmaking is FIFO with no player identity; one browser can open several games.
