// Pure helpers for placing ships on the client. The server validates again: it is the authority.

export const DIRS = { HORIZONTAL: [0, 1], VERTICAL: [1, 0] };

/** Flat index of a cell: row * size + col (same addressing as the API's board strings). */
export const idx = (row, col, size) => row * size + col;

export function cellsOf(p) {
  const [dr, dc] = DIRS[p.orientation];
  const cells = [];
  for (let k = 0; k < p.size; k++) cells.push([p.row + k * dr, p.col + k * dc]);
  return cells;
}

export function fits(p, boardSize) {
  const [dr, dc] = DIRS[p.orientation];
  const endRow = p.row + (p.size - 1) * dr;
  const endCol = p.col + (p.size - 1) * dc;
  return p.row >= 0 && p.col >= 0 && endRow < boardSize && endCol < boardSize;
}

/** Cell -> true for every cell used by the placed ships, ignoring ship number `skip`. */
function occupancy(placed, boardSize, skip = -1) {
  const used = new Uint8Array(boardSize * boardSize);
  placed.forEach((p, i) => {
    if (!p || i === skip) return;
    for (const [r, c] of cellsOf(p)) used[idx(r, c, boardSize)] = 1;
  });
  return used;
}

export function canPlace(placed, index, candidate, boardSize) {
  if (!fits(candidate, boardSize)) return false;
  const used = occupancy(placed, boardSize, index);
  return cellsOf(candidate).every(([r, c]) => !used[idx(r, c, boardSize)]);
}

/** Board string in the same format as the API: '.' water, 'S' ship. */
export function renderPlacement(placed, boardSize) {
  const out = new Array(boardSize * boardSize).fill('.');
  for (const p of placed) {
    if (!p) continue;
    for (const [r, c] of cellsOf(p)) out[idx(r, c, boardSize)] = 'S';
  }
  return out.join('');
}

/** Random valid fleet; `sizes` is the fleet from the server, e.g. [3, 4, 5]. */
export function randomFleet(sizes, boardSize, rng = Math.random) {
  for (let attempt = 0; attempt < 100; attempt++) {
    const placed = new Array(sizes.length).fill(null);
    let ok = true;
    for (let i = 0; i < sizes.length && ok; i++) {
      ok = false;
      for (let tries = 0; tries < 200 && !ok; tries++) {
        const candidate = {
          size: sizes[i],
          row: Math.floor(rng() * boardSize),
          col: Math.floor(rng() * boardSize),
          orientation: rng() < 0.5 ? 'HORIZONTAL' : 'VERTICAL',
        };
        if (canPlace(placed, i, candidate, boardSize)) {
          placed[i] = candidate;
          ok = true;
        }
      }
    }
    if (ok) return placed;
  }
  throw new Error('could not place fleet');
}
