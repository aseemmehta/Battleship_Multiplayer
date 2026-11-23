import { useState } from 'react';
import Lobby from './components/Lobby.jsx';
import GameScreen from './components/GameScreen.jsx';
import { clearSession, loadSession, saveSession } from './session.js';

export default function App() {
  const [session, setSession] = useState(loadSession);

  const start = (next) => {
    saveSession(next);
    setSession(next);
  };
  const leave = () => {
    clearSession();
    setSession(null);
  };

  return (
    <main className="app">
      <h1 className={session ? 'brand small' : 'brand'}>Battleship</h1>
      {session ? (
        <GameScreen key={session.gameId} session={session} onLeave={leave} />
      ) : (
        <Lobby onStart={start} />
      )}
    </main>
  );
}
