// Remembers the running game so a reload (or a phone waking up) does not lose it.
// Open the page with ?as=2 to keep a second, independent player in the same browser.

const profile = new URLSearchParams(window.location.search).get('as') || 'default';
const SESSION_KEY = `battleship.session.${profile}`;
const NAME_KEY = `battleship.name.${profile}`;

function read(key) {
  try {
    return JSON.parse(window.localStorage.getItem(key));
  } catch {
    return null;
  }
}

function write(key, value) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* private mode: the game still works, it just will not survive a reload */
  }
}

export const loadSession = () => read(SESSION_KEY);
export const saveSession = (session) => write(SESSION_KEY, session);
export const clearSession = () => {
  try {
    window.localStorage.removeItem(SESSION_KEY);
  } catch {
    /* ignore */
  }
};
export const loadName = () => read(NAME_KEY) || '';
export const saveName = (name) => write(NAME_KEY, name);
