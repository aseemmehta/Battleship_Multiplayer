import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../api.js';

const POLL_MS = 1000;

/**
 * Polls the game state. The API answers 304 (no body) while nothing changed, so a 1 s poll is cheap.
 * Polling pauses while the tab is hidden (saves phone battery) and stops once the game is finished.
 */
export function useGame(session, onGone) {
  const [game, setGameState] = useState(null);
  const [error, setError] = useState(null);
  const gone = useRef(onGone);
  gone.current = onGone;

  // Only accept newer data, so a slow poll can never overwrite the answer to a move.
  const setGame = useCallback((next) => {
    setGameState((prev) => (!prev || next.version >= prev.version ? next : prev));
  }, []);

  const refresh = useCallback(async () => {
    try {
      const next = await api.getGame(session.gameId, session.token);
      setGame(next);
      setError(null);
      return next;
    } catch (e) {
      if (e.status === 404 || e.status === 403) gone.current?.();
      else setError(e.message);
      return null;
    }
  }, [session.gameId, session.token, setGame]);

  useEffect(() => {
    let stopped = false;
    let timer;
    const tick = async () => {
      if (stopped) return;
      let finished = false;
      if (!document.hidden) {
        const next = await refresh();
        finished = next?.status === 'FINISHED';
      }
      if (!stopped && !finished) timer = setTimeout(tick, POLL_MS);
    };
    tick();
    const onVisible = () => {
      if (!document.hidden) {
        clearTimeout(timer);
        tick();
      }
    };
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      stopped = true;
      clearTimeout(timer);
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [refresh]);

  return { game, error, refresh, setGame };
}
