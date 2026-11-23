import { useState } from 'react';
import { api } from '../api.js';
import { useGame } from '../hooks/useGame.js';
import Banner from './Banner.jsx';
import Battle from './Battle.jsx';
import Placement from './Placement.jsx';
import Toast from './Toast.jsx';

export default function GameScreen({ session, onLeave }) {
  const { game, error, refresh, setGame } = useGame(session, onLeave);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null); // { id, title, message } of the last failed action

  async function act(call, title) {
    setBusy(true);
    setNotice(null);
    try {
      setGame(await call());
    } catch (e) {
      setNotice({ id: Date.now(), title, message: e.message });
      refresh();
    } finally {
      setBusy(false);
    }
  }

  const placeFleet = (ships) => act(() => api.placeFleet(session.gameId, session.token, ships), 'Fleet not saved');
  const fire = (row, col) => act(() => api.fire(session.gameId, session.token, row, col), 'Shot not fired');

  if (!game) {
    return (
      <div className="narrow">
        <Banner
          tone={error ? 'theirs' : 'info'}
          icon={error ? 'wait' : 'radar'}
          title={error ? 'Can’t reach the game' : 'Connecting…'}
          sub={error || undefined}
        />
      </div>
    );
  }

  return (
    <>
      <Toast key={notice?.id ?? 'none'} notice={notice} onDone={() => setNotice(null)} />

      {error && (
        <p className="conn" role="status">
          Connection problem: {error} Retrying…
        </p>
      )}

      {game.status === 'WAITING' && (
        <div className="narrow">
          <Banner tone="info" icon="radar" title="Looking for an opponent…" sub="You’ll be paired automatically" />
          <p className="hint" style={{ marginTop: 14 }}>
            On another phone or tab, open this same page and press “Find an opponent”. (Same browser? Add{' '}
            <code>?as=2</code> to the address.)
          </p>
        </div>
      )}
      {game.status === 'PLACING' && <Placement game={game} busy={busy} onSubmit={placeFleet} />}
      {(game.status === 'IN_PROGRESS' || game.status === 'FINISHED') && (
        <Battle game={game} busy={busy} failTick={notice?.id} onFire={fire} onLeave={onLeave} />
      )}

      {game.status !== 'FINISHED' && (
        <button type="button" className="link" onClick={onLeave}>
          Leave game
        </button>
      )}
    </>
  );
}
