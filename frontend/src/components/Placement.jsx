import { useMemo, useState } from 'react';
import Board from './Board.jsx';
import { canPlace, randomFleet, renderPlacement } from '../lib/fleet.js';

export default function Placement({ game, busy, onSubmit }) {
  const size = game.boardSize;
  const [placed, setPlaced] = useState(() => new Array(game.fleet.length).fill(null));
  const [current, setCurrent] = useState(0);
  const [orientation, setOrientation] = useState('HORIZONTAL');

  const cells = useMemo(() => renderPlacement(placed, size), [placed, size]);
  const allPlaced = placed.every(Boolean);

  function put(row, col) {
    const candidate = { size: game.fleet[current], row, col, orientation };
    if (!canPlace(placed, current, candidate, size)) return;
    const next = placed.slice();
    next[current] = candidate;
    setPlaced(next);
    const open = next.findIndex((p) => !p);
    if (open !== -1) setCurrent(open);
  }

  if (game.fleetPlaced) {
    return (
      <section>
        <p className="status">Fleet ready. Waiting for {game.opponentName || 'the opponent'} to place theirs…</p>
        <Board cells={game.ownBoard} size={size} />
      </section>
    );
  }

  return (
    <section>
      <p className="status">
        {game.opponentName} joined. Place your ships, then press Ready.
      </p>
      <div className="row">
        {game.fleet.map((shipSize, i) => (
          <button
            key={i}
            type="button"
            className={`chip${i === current ? ' on' : ''}${placed[i] ? ' done' : ''}`}
            onClick={() => setCurrent(i)}
          >
            Ship {shipSize}
            {placed[i] ? ' ✓' : ''}
          </button>
        ))}
      </div>
      <div className="row">
        <button type="button" onClick={() => setOrientation(orientation === 'HORIZONTAL' ? 'VERTICAL' : 'HORIZONTAL')}>
          {orientation === 'HORIZONTAL' ? '↔ Horizontal' : '↕ Vertical'}
        </button>
        <button type="button" onClick={() => setPlaced(randomFleet(game.fleet, size))}>
          🎲 Random
        </button>
        <button type="button" onClick={() => setPlaced(new Array(game.fleet.length).fill(null))}>
          Clear
        </button>
      </div>
      <Board cells={cells} size={size} onCell={put} clickable={() => true} />
      <p className="hint">Tap a cell to put the selected ship there (it extends right or down).</p>
      <button className="primary" type="button" disabled={!allPlaced || busy} onClick={() => onSubmit(placed)}>
        Ready
      </button>
    </section>
  );
}
