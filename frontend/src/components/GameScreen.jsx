import { useState } from 'react';
import { api } from '../api.js';
import { useGame } from '../hooks/useGame.js';
import Battle from './Battle.jsx';
import Placement from './Placement.jsx';

export default function GameScreen({ session, onLeave }) {
  const { game, error, refresh, setGame } = useGame(session, onLeave);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState(null);

  async function act(call) {
    setBusy(true);
    setActionError(null);
    try {
      setGame(await call());
    } catch (e) {
      setActionError(e.message);
      refresh();
    } finally {
      setBusy(false);
    }
  }

  const placeFleet = (ships) => act(() => api.placeFleet(session.gameId, session.token, ships));
  const fire = (row, col) => act(() => api.fire(session.gameId, session.token, row, col));

  if (!game) {
    return <p className="status">{error || 'Loading…'}</p>;
  }

  return (
    <>
      {game.status === 'WAITING' && (
        <section className="card">
          <p className="status">Waiting for an opponent…</p>
          <p className="hint">
            On another phone or tab, open this same page and press “Find an opponent”.
            {' '}(Same browser? Add <code>?as=2</code> to the address.)
          </p>
        </section>
      )}
      {game.status === 'PLACING' && <Placement game={game} busy={busy} onSubmit={placeFleet} />}
      {(game.status === 'IN_PROGRESS' || game.status === 'FINISHED') && (
        <Battle game={game} busy={busy} onFire={fire} onLeave={onLeave} />
      )}

      {(error || actionError) && <p className="error">{actionError || error}</p>}
      {game.status !== 'FINISHED' && (
        <button type="button" className="link" onClick={onLeave}>
          Leave game
        </button>
      )}
    </>
  );
}
