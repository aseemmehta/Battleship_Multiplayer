import { useState } from 'react';
import { api } from '../api.js';
import { loadName, saveName } from '../session.js';

export default function Lobby({ onStart }) {
  const [name, setName] = useState(loadName);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function find(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const trimmed = name.trim();
      saveName(trimmed);
      const joined = await api.join(trimmed || 'Player');
      onStart({ gameId: joined.gameId, token: joined.token, slot: joined.slot });
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  }

  return (
    <div className="narrow">
      <p className="tagline">Two players. One sea. Sink the fleet before yours goes down.</p>
      <form className="card" onSubmit={find}>
        <label htmlFor="name">Your name</label>
        <input
          id="name"
          value={name}
          maxLength={30}
          autoComplete="nickname"
          placeholder="Captain"
          onChange={(e) => setName(e.target.value)}
        />
        <button className="primary" type="submit" disabled={busy}>
          {busy ? 'Looking…' : 'Find an opponent'}
        </button>
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        <p className="hint">Server: {api.base}</p>
      </form>
    </div>
  );
}
