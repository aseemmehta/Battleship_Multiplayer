import { useState } from 'react';
import Board, { coord } from './Board.jsx';

const RESULT_TEXT = { MISS: 'missed', HIT: 'hit a ship', SUNK: 'sank a ship' };

function headline(game) {
  if (game.status === 'FINISHED') {
    if (game.result === 'WON') return game.endReason === 'TIMEOUT' ? '🏆 You win: opponent ran out of time' : '🏆 You won!';
    if (game.result === 'LOST') return game.endReason === 'TIMEOUT' ? 'You lose: you ran out of time' : 'You lost';
    return 'Game ended (nobody finished placing ships)';
  }
  return game.yourTurn ? 'Your turn: pick a target' : `${game.opponentName}'s turn…`;
}

export default function Battle({ game, busy, onFire, onLeave }) {
  const [target, setTarget] = useState(null); // flat index
  const size = game.boardSize;
  const finished = game.status === 'FINISHED';
  const canShoot = game.yourTurn && !busy;

  const last = game.lastShot;
  const lastText = last
    ? `${last.bySlot === game.yourSlot ? 'You' : game.opponentName} fired at ${coord(last.row, last.col)} and ${RESULT_TEXT[last.result]}`
    : null;

  function fire() {
    if (target === null) return;
    onFire(Math.floor(target / size), target % size);
    setTarget(null);
  }

  return (
    <section>
      <p className={`status${game.yourTurn ? ' go' : ''}`}>{headline(game)}</p>
      {lastText && <p className="hint">{lastText}</p>}

      <h2>Enemy waters · {game.yourHits}/{game.totalShipCells} hit</h2>
      <Board
        cells={game.enemyBoard}
        size={size}
        onCell={(row, col) => setTarget(row * size + col)}
        clickable={(ch) => canShoot && ch === '.'}
        selected={target ?? -1}
      />
      {!finished && (
        <button className="primary" type="button" disabled={!canShoot || target === null} onClick={fire}>
          {target === null ? 'Fire!' : `Fire at ${coord(Math.floor(target / size), target % size)}!`}
        </button>
      )}

      <h2>Your fleet · {game.enemyHits}/{game.totalShipCells} lost</h2>
      <Board cells={game.ownBoard} size={size} small />

      {finished && (
        <button className="primary" type="button" onClick={onLeave}>
          Play again
        </button>
      )}
    </section>
  );
}
