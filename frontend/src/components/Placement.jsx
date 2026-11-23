import { useMemo, useState } from 'react';
import Banner from './Banner.jsx';
import Board from './Board.jsx';
import { canPlace, cellsOf, idx, randomFleet, renderPlacement } from '../lib/fleet.js';

export default function Placement({ game, busy, onSubmit }) {
  const size = game.boardSize;
  const [placed, setPlaced] = useState(() => new Array(game.fleet.length).fill(null));
  const [current, setCurrent] = useState(0);
  const [orientation, setOrientation] = useState('HORIZONTAL');
  const [hover, setHover] = useState(null); // [row, col] while a mouse is over the board
  const [misfit, setMisfit] = useState(false);
  const [shakeKey, setShakeKey] = useState(0); // bumps on every failed tap so the shake replays

  const cells = useMemo(() => renderPlacement(placed, size), [placed, size]);
  const allPlaced = placed.every(Boolean);

  // Live preview under the mouse: green = fits, red = does not.
  const ghost = useMemo(() => {
    if (!hover) return null;
    const candidate = { size: game.fleet[current], row: hover[0], col: hover[1], orientation };
    const inside = cellsOf(candidate).filter(([r, c]) => r < size && c < size);
    return { cells: new Set(inside.map(([r, c]) => idx(r, c, size))), ok: canPlace(placed, current, candidate, size) };
  }, [hover, placed, current, orientation, game.fleet, size]);

  function put(row, col) {
    const candidate = { size: game.fleet[current], row, col, orientation };
    if (!canPlace(placed, current, candidate, size)) {
      setMisfit(true); // not silent any more: the board shakes and the hint turns red
      setShakeKey((k) => k + 1);
      return;
    }
    setMisfit(false);
    const next = placed.slice();
    next[current] = candidate;
    setPlaced(next);
    const open = next.findIndex((p) => !p);
    if (open !== -1) setCurrent(open);
  }

  if (game.fleetPlaced) {
    return (
      <section>
        <div className="sticky-top">
          <Banner tone="info" icon="check" title="Fleet ready" sub={`Waiting for ${game.opponentName || 'the opponent'} to place theirs…`} />
        </div>
        <div className="panel narrow">
          <Board variant="own" cells={game.ownBoard} size={size} />
        </div>
      </section>
    );
  }

  return (
    <section>
      <div className="sticky-top">
        <Banner tone="info" icon="ship" title={`${game.opponentName || 'Opponent'} joined`} sub="Place your ships, then press Ready" />
      </div>

      <div className="setup">
        <div className="a-side panel">
          <div className="eyebrow" style={{ marginBottom: 8 }}>Your ships</div>
          <div className="row">
            {game.fleet.map((shipSize, i) => (
              <button
                key={i}
                type="button"
                className={`chip${i === current ? ' on' : ''}${placed[i] ? ' done' : ''}`}
                onClick={() => setCurrent(i)}
              >
                <span className="shipbar" aria-hidden="true">
                  {Array.from({ length: shipSize }, (_, k) => (
                    <i key={k} />
                  ))}
                </span>
                Ship {shipSize}
                {placed[i] && <span className="tick">✓</span>}
              </button>
            ))}
          </div>
          <div className="row">
            <button type="button" onClick={() => setOrientation(orientation === 'HORIZONTAL' ? 'VERTICAL' : 'HORIZONTAL')}>
              {orientation === 'HORIZONTAL' ? '↔ Horizontal' : '↕ Vertical'}
            </button>
            <button
              type="button"
              onClick={() => {
                setMisfit(false);
                setPlaced(randomFleet(game.fleet, size));
              }}
            >
              🎲 Random
            </button>
            <button
              type="button"
              onClick={() => {
                setMisfit(false);
                setPlaced(new Array(game.fleet.length).fill(null));
                setCurrent(0);
              }}
            >
              Clear
            </button>
          </div>
          <p className={`hint${misfit ? ' bad' : ''}`} role={misfit ? 'alert' : undefined}>
            {misfit
              ? 'That ship does not fit there. Try another spot, or switch direction.'
              : 'Tap a cell to place the selected ship (it extends right or down).'}
          </p>
        </div>

        <div className="a-board panel">
          <div key={shakeKey} className={shakeKey ? 'shake' : undefined}>
            <Board
              variant="setup"
              cells={cells}
              size={size}
              onCell={put}
              clickable={() => true}
              ghost={ghost}
              onHover={(row, col) => setHover(row === null || row === undefined ? null : [row, col])}
            />
          </div>
        </div>

        <div className="a-ready fire-bar">
          <button className="primary go" type="button" disabled={!allPlaced || busy} onClick={() => onSubmit(placed)}>
            {allPlaced ? 'Ready' : `Place ${placed.filter((p) => !p).length} more`}
          </button>
        </div>
      </div>
    </section>
  );
}
