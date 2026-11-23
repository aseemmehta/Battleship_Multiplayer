-- Current state of every game. Boards are not stored as 100-cell grids: they are derived from
-- ships (where they are, how often they were hit) and shots (what was fired at which cell).

create table games (
    id                uuid         primary key,
    status            varchar(20)  not null,
    current_turn_slot integer,
    winner_slot       integer,
    end_reason        varchar(20),
    created_at        timestamptz  not null,
    updated_at        timestamptz  not null,
    version           bigint       not null default 0
);

-- matchmaking: "oldest waiting game"
create index idx_games_waiting on games (created_at) where status = 'WAITING';
-- janitor: "games of status X idle since T"
create index idx_games_status_updated on games (status, updated_at);

create table players (
    id           uuid        primary key,
    game_id      uuid        not null references games (id) on delete cascade,
    slot         integer     not null check (slot in (1, 2)),
    name         varchar(30) not null,
    token        uuid        not null,
    fleet_placed boolean     not null default false,
    constraint uq_players_game_slot unique (game_id, slot),
    constraint uq_players_token unique (token)
);

create table ships (
    id          bigserial   primary key,
    player_id   uuid        not null references players (id) on delete cascade,
    size        integer     not null check (size > 0),
    origin_row  integer     not null,
    origin_col  integer     not null,
    orientation varchar(10) not null,
    hits        integer     not null default 0
);
create index idx_ships_player on ships (player_id);

create table shots (
    id           bigserial   primary key,
    game_id      uuid        not null references games (id) on delete cascade,
    shooter_slot integer     not null check (shooter_slot in (1, 2)),
    cell         integer     not null check (cell >= 0),
    result       varchar(10) not null,
    fired_at     timestamptz not null,
    -- the database itself refuses a second shot at the same cell
    constraint uq_shots_cell unique (game_id, shooter_slot, cell)
);
