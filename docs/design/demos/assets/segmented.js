/** Sliding pill segmented control */
export function initSegmentedTrack(trackEl, onChange) {
  if (!trackEl) return null;

  const segments = [...trackEl.querySelectorAll('.segment')];
  const pill = trackEl.querySelector('.segment-pill');
  if (!segments.length || !pill) return null;

  let selected = segments.findIndex((s) => s.classList.contains('is-selected'));
  if (selected < 0) selected = 0;

  pill.style.visibility = 'hidden';

  const measurePill = (index) => {
    const seg = segments[index];
    if (!seg) return null;
    // Force layout so flex widths are current before measuring.
    trackEl.getBoundingClientRect();
    const trackRect = trackEl.getBoundingClientRect();
    const segRect = seg.getBoundingClientRect();
    return {
      left: segRect.left - trackRect.left,
      width: segRect.width,
    };
  };

  const movePill = (index, animate = true) => {
    const seg = segments[index];
    if (!seg) return;
    const bounds = measurePill(index);
    if (!bounds || bounds.width <= 0) return;

    segments.forEach((s, i) => s.classList.toggle('is-selected', i === index));
    pill.style.transition = animate
      ? 'left var(--transition-segment), width var(--transition-segment)'
      : 'none';
    pill.style.left = `${bounds.left}px`;
    pill.style.width = `${bounds.width}px`;
    pill.style.visibility = 'visible';
    selected = index;
    onChange?.(index, seg.dataset.value);
  };

  const ac = new AbortController();
  const { signal } = ac;

  segments.forEach((seg, i) => {
    seg.addEventListener('click', () => movePill(i), { signal });
  });

  const relayout = () => movePill(selected, false);
  const ro = new ResizeObserver(relayout);
  ro.observe(trackEl);

  movePill(selected, false);
  requestAnimationFrame(() => {
    movePill(selected, false);
  });

  return {
    movePill,
    getSelected: () => selected,
    destroy: () => {
      ac.abort();
      ro.disconnect();
    },
  };
}
