// The browser talks to the gateway only. By default that is the host the page was loaded from, port 9090,
// so the same build works on localhost and on a phone using http://<your-LAN-IP>:3000.
const GATEWAY_PORT = import.meta.env.VITE_GATEWAY_PORT || '9090';
const BASE = (
  import.meta.env.VITE_API_BASE || `${window.location.protocol}//${window.location.hostname}:${GATEWAY_PORT}`
).replace(/\/$/, '');

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function request(path, { method = 'GET', token, body } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['X-Player-Token'] = token;

  let response;
  try {
    response = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the game server. Are you on the same Wi-Fi?');
  }

  if (!response.ok) {
    let detail = response.statusText;
    try {
      const problem = await response.json();
      detail = problem.detail || problem.title || detail;
    } catch {
      /* not JSON */
    }
    throw new ApiError(response.status, detail);
  }
  return response.json();
}

export const api = {
  base: BASE,
  join: (name) => request('/api/matchmaking', { method: 'POST', body: { name } }),
  getGame: (gameId, token) => request(`/api/games/${gameId}`, { token }),
  placeFleet: (gameId, token, ships) =>
    request(`/api/games/${gameId}/fleet`, { method: 'PUT', token, body: { ships } }),
  fire: (gameId, token, row, col) =>
    request(`/api/games/${gameId}/shots`, { method: 'POST', token, body: { row, col } }),
};
