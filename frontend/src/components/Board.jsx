import { useEffect, useRef } from 'react';

const KIND = { '.': 'water', S: 'ship', H: 'hit', M: 'miss', X: 'sunk' };
const LABEL = { '.': 'water', S: 'your ship', H: 'hit', M: 'miss', X: 'sunk ship' };
const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

export const coord = (row, col) => `${LETTERS[col]}${row + 1}`;

const star = (points, outer, inner) =>
  Array.from({ length: points * 2 }, (_, i) => {
    const angle = (Math.PI * i) / points - Math.PI / 2;
    const r = i % 2 ? inner : outer;
    return `${(12 + r * Math.cos(angle)).toFixed(2)},${(12 + r * Math.sin(angle)).toFixed(2)}`;
  }).join(' ');

const HIT_STAR = star(8, 10, 5);
const SUNK_STAR = star(12, 11, 6.5);

/**
 * Shapes carry the meaning, not just colour:
 *  hit  = 8-point burst   (you hit something)
 *  sunk = 12-point burst with a core, gold on crimson (you destroyed a ship; deliberately not an "X")
 *  miss = hollow ring     (splash)
 *  aim  = crosshair       (selected target)
 */
export function CellIcon({ kind }) {
  switch (kind) {
    case 'hit':
      return (
        <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
          <polygon points={HIT_STAR} fill="currentColor" />
        </svg>
      );
    case 'sunk':
      return (
        <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
          <polygon points={SUNK_STAR} fill="currentColor" />
          <circle cx="12" cy="12" r="3.2" fill="var(--sunk-core, #8f0f2a)" />
        </svg>
      );
    case 'miss':
      return (
        <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="5" fill="none" stroke="currentColor" strokeWidth="2.4" />
        </svg>
      );
    case 'aim':
      return (
        <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" aria-hidden="true">
          <circle cx="12" cy="12" r="6.5" />
          <path d="M12 2v5M12 17v5M2 12h5M17 12h5" />
        </svg>
      );
    default:
      return null;
  }
}

/**
 * cells: string of size*size chars, row-major ('.' water, 'S' ship, 'H' hit, 'M' miss, 'X' sunk).
 * onCell(row, col) is only called for cells where `clickable(char)` is true.
 * variant: 'enemy' | 'own' | 'setup' (picks the water colour).
 * last: flat index of the most recent shot on this board (gets a ring).
 * ghost: { cells: Set<flatIndex>, ok } placement preview.
 */
export default function Board({
  cells,
  size,
  variant = 'enemy',
  onCell,
  clickable = () => false,
  selected = -1,
  last = -1,
  labels = true,
  ghost = null,
  onHover,
}) {
  // Cells that changed since the previous render get a one-off reveal animation.
  const prev = useRef(null);
  const before = prev.current;
  useEffect(() => {
    prev.current = cells;
  }, [cells]);

  const nodes = [];
  if (labels) {
    nodes.push(<span key="corner" className="axis" />);
    for (let c = 0; c < size; c++) {
      nodes.push(
        <span key={`c${c}`} className="axis" aria-hidden="true">
          {LETTERS[c]}
        </span>,
      );
    }
  }

  for (let row = 0; row < size; row++) {
    if (labels) {
      nodes.push(
        <span key={`r${row}`} className="axis" aria-hidden="true">
          {row + 1}
        </span>,
      );
    }
    for (let col = 0; col < size; col++) {
      const i = row * size + col;
      const ch = cells[i] || '.';
      const kind = KIND[ch];
      const active = Boolean(onCell) && clickable(ch);
      const isSelected = selected === i;
      const fresh = before !== null && before !== cells && (before[i] || '.') !== ch;
      const ghostClass = ghost && ghost.cells.has(i) ? (ghost.ok ? ' ghost-ok' : ' ghost-bad') : '';
      nodes.push(
        <button
          key={i}
          type="button"
          className={`cell ${kind}${isSelected ? ' selected' : ''}${last === i ? ' last' : ''}${fresh ? ' fresh' : ''}${ghostClass}`}
          disabled={!active}
          aria-label={`${coord(row, col)}: ${LABEL[ch]}`}
          aria-pressed={isSelected || undefined}
          onClick={() => onCell(row, col)}
          onPointerEnter={(e) => e.pointerType === 'mouse' && onHover?.(row, col)}
        >
          <CellIcon kind={isSelected ? 'aim' : kind} />
        </button>,
      );
    }
  }

  return (
    <div
      className={`board ${variant}`}
      style={{ gridTemplateColumns: `${labels ? '1.5em ' : ''}repeat(${size}, minmax(0, 1fr))` }}
      onPointerLeave={() => onHover?.(null)}
    >
      {nodes}
    </div>
  );
}
