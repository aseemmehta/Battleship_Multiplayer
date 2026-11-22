const KIND = { '.': 'water', S: 'ship', H: 'hit', M: 'miss', X: 'sunk' };
const SYMBOL = { '.': '', S: '', H: '✕', M: '•', X: '✕' };
const LABEL = { '.': 'water', S: 'your ship', H: 'hit', M: 'miss', X: 'sunk ship' };

export const coord = (row, col) => `${String.fromCharCode(65 + col)}${row + 1}`;

/**
 * cells: string of size*size chars, row-major ('.' water, 'S' ship, 'H' hit, 'M' miss, 'X' sunk).
 * onCell(row, col) is only called for cells where `clickable(char)` is true.
 */
export default function Board({ cells, size, onCell, clickable = () => false, selected = -1, small = false }) {
  const buttons = [];
  for (let i = 0; i < size * size; i++) {
    const ch = cells[i] || '.';
    const row = Math.floor(i / size);
    const col = i % size;
    const active = onCell && clickable(ch);
    buttons.push(
      <button
        key={i}
        type="button"
        className={`cell ${KIND[ch]}${selected === i ? ' selected' : ''}`}
        disabled={!active}
        aria-label={`${coord(row, col)}: ${LABEL[ch]}`}
        onClick={() => onCell(row, col)}
      >
        {SYMBOL[ch]}
      </button>,
    );
  }
  return (
    <div className={`grid${small ? ' small' : ''}`} style={{ gridTemplateColumns: `repeat(${size}, 1fr)` }}>
      {buttons}
    </div>
  );
}
