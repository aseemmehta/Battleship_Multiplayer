import { useEffect, useRef, useState } from 'react';
import Banner from './Banner.jsx';
import Board, { CellIcon, coord } from './Board.jsx';
import ResultSheet from './ResultSheet.jsx';

const buzz = (pattern) => {
  try {
    navigator.vibrate?.(pattern);
  } catch {
    /* not supported: fine */
  }
};

function bannerFor(game, busy) {
  const opp = game.opponentName || 'Opponent';
  if (game.status === 'FINISHED') {
    if (game.result === 'WON') return { tone: 'won', icon: 'trophy', title: 'You won!', sub: game.endReason === 'TIMEOUT' ? `${opp} ran out of time` : 'Enemy fleet destroyed' };
    if (game.result === 'LOST') return { tone: 'lost', icon: 'anchor', title: 'You lost', sub: game.endReason === 'TIMEOUT' ? 'You ran out of time' : 'Your fleet was sunk' };
    return { tone: 'ended', icon: 'anchor', title: 'Game ended', sub: 'Nobody finished placing ships' };
  }
  if (game.yourTurn) {
    return { tone: 'mine', icon: 'aim', title: 'Your turn', sub: busy ? 'Firing…' : 'Pick a target in enemy waters' };
  }
  return { tone: 'theirs', icon: 'wait', title: `${opp}’s turn`, sub: 'Waiting for their shot…' };
}

const FEED = {
  mine: {
    SUNK: { tint: '255 214 10', kind: 'sunk', title: 'You sank a ship!' },
    HIT: { tint: '255 159 10', kind: 'hit', title: 'Direct hit!' },
    MISS: { tint: '120 160 210', kind: 'miss', title: 'Miss' },
  },
  theirs: {
    SUNK: { tint: '255 69 58', kind: 'sunk', title: 'Your ship was sunk' },
    HIT: { tint: '255 99 71', kind: 'hit', title: 'Your ship was hit' },
    MISS: { tint: '100 210 255', kind: 'miss', title: 'They missed' },
  },
};

function ShotFeed({ game }) {
  const last = game.lastShot;
  if (!last) return null;
  const mine = last.bySlot === game.yourSlot;
  const style = FEED[mine ? 'mine' : 'theirs'][last.result];
  const who = mine ? 'You fired' : `${game.opponentName || 'Opponent'} fired`;
  return (
    <div className="feed" key={`${last.bySlot}-${last.row}-${last.col}`} style={{ '--tint': style.tint }}>
      <span className={`swatch ${style.kind}`}>
        <CellIcon kind={style.kind} />
      </span>
      <div>
        <div className="feed-title">{style.title}</div>
        <div className="feed-sub">
          {who} at {coord(last.row, last.col)}
        </div>
      </div>
    </div>
  );
}

function Meter({ value, max, bad }) {
  const pct = max ? Math.min(100, Math.round((value / max) * 100)) : 0;
  return (
    <div className={`meter${bad ? ' bad' : ''}`} role="progressbar" aria-valuemin={0} aria-valuemax={max} aria-valuenow={value}>
      <i style={{ '--p': `${pct}%` }} />
    </div>
  );
}

function Legend({ own }) {
  const items = [
    ['hit', 'Hit'],
    ['sunk', 'Sunk'],
    ['miss', 'Miss'],
  ];
  return (
    <div className="legend" aria-hidden="true">
      {own && (
        <span className="item">
          <span className="swatch ship" /> Ship
        </span>
      )}
      {items.map(([kind, label]) => (
        <span className="item" key={kind}>
          <span className={`swatch ${kind}`}>
            <CellIcon kind={kind} />
          </span>
          {label}
        </span>
      ))}
    </div>
  );
}

export default function Battle({ game, busy, failTick, onFire, onLeave }) {
  const [target, setTarget] = useState(null); // flat index
  const [shake, setShake] = useState(false);
  const [sheetOpen, setSheetOpen] = useState(true);

  const size = game.boardSize;
  const finished = game.status === 'FINISHED';
  const mine = game.yourTurn && !finished;
  const canShoot = game.yourTurn && !busy;
  const opponent = game.opponentName || 'Opponent';

  // Haptic + tab title the moment it becomes your turn (same moment as the banner changes).
  const wasMine = useRef(game.yourTurn);
  useEffect(() => {
    if (game.yourTurn && !wasMine.current) buzz(14);
    wasMine.current = game.yourTurn;
  }, [game.yourTurn]);

  useEffect(() => {
    document.title = mine ? '● Your turn · Battleship' : 'Battleship';
    return () => {
      document.title = 'Battleship';
    };
  }, [mine]);

  useEffect(() => {
    if (finished && game.result === 'WON') buzz([18, 60, 18]);
  }, [finished, game.result]);

  // A failed move shakes the enemy board (the toast says why).
  const seenFail = useRef(failTick);
  useEffect(() => {
    if (failTick && failTick !== seenFail.current) {
      seenFail.current = failTick;
      setShake(true);
      buzz(40);
    }
  }, [failTick]);

  const last = game.lastShot;
  const lastOnEnemy = last && last.bySlot === game.yourSlot ? last.row * size + last.col : -1;
  const lastOnOwn = last && last.bySlot !== game.yourSlot ? last.row * size + last.col : -1;

  function fire() {
    if (target === null) return;
    onFire(Math.floor(target / size), target % size);
    setTarget(null);
  }

  const b = bannerFor(game, busy);
  let fireLabel;
  if (busy) fireLabel = 'Firing…';
  else if (target !== null) fireLabel = `Fire at ${coord(Math.floor(target / size), target % size)}`;
  else if (game.yourTurn) fireLabel = 'Select a target';
  else fireLabel = `Waiting for ${opponent}…`;

  return (
    <section className="battle-wrap">
      <div className="sticky-top">
        <Banner key={`${b.tone}-${b.title}`} {...b} />
      </div>

      <div className="battle-grid">
        {last && (
          <div className="a-feed">
            <ShotFeed game={game} />
          </div>
        )}

        <div className="a-enemy">
          <div className={`panel enemy${mine ? ' active' : ''}${!mine && !finished ? ' dim' : ''}`}>
            <div className="panel-head">
              <h2 className="panel-title">Enemy waters</h2>
              <span className="count">
                {game.yourHits}/{game.totalShipCells} hit
              </span>
            </div>
            <Meter value={game.yourHits} max={game.totalShipCells} />
            <div className={shake ? 'shake' : undefined} onAnimationEnd={(e) => e.target === e.currentTarget && setShake(false)}>
              <Board
                variant="enemy"
                cells={game.enemyBoard}
                size={size}
                onCell={(row, col) => setTarget(row * size + col)}
                clickable={(ch) => canShoot && ch === '.'}
                selected={target ?? -1}
                last={lastOnEnemy}
              />
            </div>
            <Legend />
          </div>

          <div className="fire-bar">
            {finished ? (
              <button className="primary" type="button" onClick={onLeave}>
                Play again
              </button>
            ) : (
              <button className="primary fire" type="button" disabled={!canShoot || target === null} onClick={fire}>
                {fireLabel}
              </button>
            )}
          </div>
        </div>

        <div className="a-own">
          <div className={`panel own${!mine && !finished ? ' watch' : ''}`}>
            <div className="panel-head">
              <h2 className="panel-title">Your fleet</h2>
              <span className="count">
                {game.enemyHits}/{game.totalShipCells} lost
              </span>
            </div>
            <Meter value={game.enemyHits} max={game.totalShipCells} bad />
            <Board variant="own" cells={game.ownBoard} size={size} labels={false} last={lastOnOwn} />
            <Legend own />
          </div>
        </div>
      </div>

      {finished && sheetOpen && (
        <ResultSheet game={game} onPlayAgain={onLeave} onClose={() => setSheetOpen(false)} />
      )}
    </section>
  );
}
