# Battleship (microservices)

Two players, two phones, one local network. Game state lives in Postgres; nothing needs `sudo`.

```
 phone / browser
   │  http://<LAN-IP>:3000          http://<LAN-IP>:9090/api/...
   ▼                                ▼
┌──────────┐                  ┌───────────┐   internal network   ┌───────┐        ┌────────────┐
│ frontend │                  │  gateway  │ ───────────────────▶ │  api  │ ─────▶ │  db        │
│ React +  │                  │  Spring   │   (not published)    │Spring │  JPA   │ PostgreSQL │
│ nginx    │                  │  Cloud GW │                      │ Boot  │        │ (volume)   │
└──────────┘                  └───────────┘                      └───────┘        └────────────┘
   :3000 (published)             :9090 (published)               :8081 (internal)    :5432 (internal)
```

| Service    | Tech                              | Published | Job |
|------------|-----------------------------------|-----------|-----|
| `frontend` | React 18, Vite, unprivileged nginx | 3000      | UI: lobby, ship placement, battle |
| `gateway`  | Spring Cloud Gateway              | 9090      | Only door to the API: routing, CORS, `X-Request-Id` |
| `api`      | Spring Boot 3, JPA, Flyway        | no        | Matchmaking, rules, turn handling |
| `db`       | PostgreSQL 16                     | no        | Current state of every game |

The old `Client/` and `Server/` (UDP, fixed IPs, `sudo`) are replaced by this. Peer-to-peer UDP is gone:
players talk to the API over HTTP, the API keeps the state.

## Run it (no sudo)

Needs only Docker (Docker Desktop on macOS/Windows works as-is; on Linux your user must be in the `docker` group,
a one-time machine setup, or use rootless Docker). No Java, Node or Maven on your machine.

```bash
docker compose up --build        # first build takes a few minutes
```

Optional: `cp .env.example .env` to change the database credentials.

Stop with `Ctrl+C`; `docker compose down` removes the containers (add `-v` to also wipe the database).

## Play from your phones

1. Put the computer and the phones on the **same Wi-Fi** (not a guest network; those often isolate devices).
2. `./scripts/lan-url.sh` prints the address, e.g. `http://192.168.1.20:3000`.
3. Open it on both phones, enter a name, tap **Find an opponent**. The second player is paired automatically.
4. Place ships (or tap 🎲 Random), tap **Ready**. Player 1 fires first. Select a cell, then **Fire!**.

Testing alone: open the page in two tabs; add `?as=2` to the second one so it keeps its own player
(`http://localhost:3000/?as=2`).

The page finds the API by itself: it calls the same host it was loaded from, on port 9090.

Calling the API directly from a phone: open `http://<LAN-IP>:9090/swagger-ui.html` and use "Try it out".

## API (through the gateway, port 9090)

| Method & path                  | Body                                   | Notes |
|--------------------------------|----------------------------------------|-------|
| `POST /api/matchmaking`        | `{ "name": "Ann" }`                    | Returns `gameId`, `token`, `slot`. Joins the oldest waiting game or opens one. |
| `GET /api/games/{id}`          | -                                      | Your view of the game; ETag = version, `304` if unchanged. |
| `PUT /api/games/{id}/fleet`    | `{ "ships": [{size,row,col,orientation}] }` | `orientation` = `HORIZONTAL` or `VERTICAL`. Sizes must be 3, 4, 5. |
| `POST /api/games/{id}/shots`   | `{ "row": 3, "col": 4 }`               | Your turn only. Returns your updated view. |

Every call except matchmaking needs the header `X-Player-Token: <token>` from the join response.
Errors are RFC 7807 JSON (`detail` holds the message). The opponent's unsunk ships are never sent to the client.

Board strings (`ownBoard`, `enemyBoard`) are `boardSize*boardSize` characters, row-major, index = `row*size + col`:
`.` water/unknown, `S` your ship, `H` hit, `M` miss, `X` sunk ship.

## Game state machine

```
WAITING --second player joins--> PLACING --both fleets placed--> IN_PROGRESS --last ship sunk--> FINISHED
   │                                │                                  │ no move for 120 s            ▲
   └──── idle 10 min ───────────────┴──────────────────────────────────┴── forfeit (TIMEOUT) ─────────┘
```
Idle timeouts live in `api/src/main/resources/application.yml` (`battleship.timeouts.*`).

## Design patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **State** | `domain/GamePhase` + `WaitingPhase`, `PlacingPhase`, `InProgressPhase`, `FinishedPhase` | "What may happen in which state" lives in one class per state; illegal commands are rejected by default |
| **Strategy** | `domain/FleetRules` (`ClassicFleetRules`) | Board size, fleet and placement validation are swappable without touching the game logic |
| **Factory method** | `Game.open(...)` | A game is always created waiting, with its creator in slot 1 |
| **Repository** | `persistence/GameRepository` (Spring Data JPA) | All SQL/locking in one place; `SKIP LOCKED` matchmaking, row locks for moves |
| **Facade / application service** | `application/GameService` | One transaction per use case; controllers stay thin |
| **Assembler (DTO)** | `application/GameViewAssembler` | The per-player view hides the opponent's ships |
| **Observer** | `GameEvent` + `GameEventLogger` | Post-commit notifications; a push channel can subscribe later without touching the rules |
| **API Gateway** | `gateway/` | One public entry, CORS and request ids in one place |
| **Database per service** | Flyway migrations live in `api/` | Only the API touches the schema |

## Data model (Postgres, `api/src/main/resources/db/migration/V1__init.sql`)

`games` (status, turn, winner, version) → `players` (slot, name, secret token, fleet placed) → `ships`
(origin, orientation, hits) and `shots` (game, shooter, cell, result). Boards are **derived** from ships + shots, so the
state is never stored twice. `UNIQUE (game_id, shooter_slot, cell)` makes a repeated shot impossible even under races.

## Development

```bash
cd api && mvn test                      # domain tests (no database needed)
cd frontend && npm install && npm run dev   # UI on :3000 against a gateway on :9090
```

## Troubleshooting

- **Phone cannot open the page**: same Wi-Fi? macOS may ask "Allow incoming connections" the first time; accept it.
  Check macOS Firewall settings for Docker. Corporate/guest networks often block device-to-device traffic.
- **Page loads but "Cannot reach the game server"**: the phone must reach port **9090** as well as 3000.
- **Address changed**: your router gave the computer a new IP; run `./scripts/lan-url.sh` again.
- **Logs**: `docker compose logs -f api gateway`.
- **`port is already allocated`**: find the owner with `lsof -nP -iTCP:9090 -sTCP:LISTEN` (or `docker ps`), stop it,
  or set `FRONTEND_PORT` / `GATEWAY_PORT` in `.env` and run `docker compose up --build` again.
- **Fresh start**: `docker compose down -v` (wipes all games).
