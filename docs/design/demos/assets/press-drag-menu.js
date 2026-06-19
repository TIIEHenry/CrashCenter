/**
 * Press-Drag-Release menu (interaction-language §按住滑动选单)
 *
 * Path A: long-press anchor → drag into menu → release on row
 * Path B: short click anchor → click row (or press-drag on row when open)
 */
export function initPressDragMenu({ anchor, menu, onSelect, statusEl, onMenuOpen }) {
  const LONG_PRESS_MS = 400;

  let session = null;
  let longPressTimer = null;
  let suppressClick = false;

  const rows = () => [...menu.querySelectorAll('[data-menu-item]')];
  const isOpen = () => !menu.hidden;

  const hitRow = (clientX, clientY) => {
    const el = document.elementFromPoint(clientX, clientY);
    return el?.closest('[data-menu-item]') ?? null;
  };

  const overAnchor = (clientX, clientY) => {
    const rect = anchor.getBoundingClientRect();
    return (
      clientX >= rect.left &&
      clientX <= rect.right &&
      clientY >= rect.top &&
      clientY <= rect.bottom
    );
  };

  const setHighlight = (row) => {
    rows().forEach((r) => r.classList.toggle('is-highlight', r === row));
    if (statusEl && row) {
      statusEl.textContent = `跟选：${row.dataset.label}`;
    }
  };

  const clearRowPress = () => {
    setHighlight(null);
    rows().forEach((r) => r.classList.remove('is-pressed'));
  };

  const open = (mode) => {
    menu.hidden = false;
    menu.removeAttribute('hidden');
    menu.dataset.mode = mode;
    if (statusEl) {
      statusEl.textContent = mode === 'drag' ? '按住滑动中…' : '菜单已打开（可点行）';
    }
    onMenuOpen?.();
  };

  const close = (triggered) => {
    menu.hidden = true;
    menu.setAttribute('hidden', '');
    clearRowPress();
    if (statusEl) {
      statusEl.textContent = triggered
        ? `已触发：${triggered.dataset.label}`
        : '松手在菜单外 → 已关闭';
    }
    session = null;
  };

  const perform = (row) => {
    if (!row) return;
    suppressClick = true;
    const keepOpen = onSelect?.(row.dataset.value, row.dataset.label, row) === false;
    clearRowPress();
    if (!keepOpen) close(row);
    window.setTimeout(() => {
      suppressClick = false;
    }, 0);
  };

  const clearLongPress = () => {
    if (longPressTimer) {
      clearTimeout(longPressTimer);
      longPressTimer = null;
    }
  };

  const releaseCapture = (target, pointerId) => {
    if (target?.hasPointerCapture?.(pointerId)) {
      target.releasePointerCapture(pointerId);
    }
  };

  const enterDrag = () => {
    if (!session || session.mode !== 'pending') return;
    clearLongPress();
    session.mode = 'drag';
    session.longPressFired = true;
    try {
      anchor.setPointerCapture(session.pointerId);
    } catch {
      /* ignore if capture unavailable */
    }
    open('drag');
  };

  const finishAnchorPointer = (e) => {
    if (!session || session.pointerId !== e.pointerId) return;
    clearLongPress();
    releaseCapture(anchor, e.pointerId);

    /** Pointer gesture already handled open/close — ignore synthesized click toggle. */
    let ignoreClick = false;

    if (session.mode === 'drag') {
      const row = hitRow(e.clientX, e.clientY);
      if (row && menu.contains(row)) {
        perform(row);
        session = null;
        return;
      }
      if (session.longPressFired && overAnchor(e.clientX, e.clientY)) {
        // Long-press release on anchor: keep menu open, no row action.
        open('click');
        ignoreClick = true;
      } else {
        close(null);
        ignoreClick = true;
      }
    } else if (session.mode === 'pending' && !session.longPressFired && !isOpen()) {
      // Short tap: open here for touch paths that skip click; suppress duplicate toggle.
      open('click');
      ignoreClick = true;
    }
    // pending + menu already open: leave click to toggle close.

    if (ignoreClick) suppressClick = true;
    session = null;
  };

  function onAnchorClick(e) {
    if (e.button !== 0) return;
    if (suppressClick) {
      suppressClick = false;
      return;
    }
    if (!isOpen()) open('click');
    else close(null);
  }

  function onPointerDown(e) {
    if (e.button !== 0) return;
    session = {
      mode: 'pending',
      pointerId: e.pointerId,
      startX: e.clientX,
      startY: e.clientY,
      longPressFired: false,
    };
    clearLongPress();
    longPressTimer = window.setTimeout(enterDrag, LONG_PRESS_MS);
  }

  function onPointerMove(e) {
    if (!session || session.pointerId !== e.pointerId) return;
    if (session.mode === 'drag') {
      setHighlight(hitRow(e.clientX, e.clientY));
    }
  }

  function onMenuClick(e) {
    if (suppressClick) {
      suppressClick = false;
      return;
    }
    const row = e.target.closest('[data-menu-item]');
    if (row && isOpen()) perform(row);
  }

  function onMenuPointerDown(e) {
    const row = e.target.closest('[data-menu-item]');
    if (!row || !isOpen()) return;
    session = { mode: 'menu-drag', pointerId: e.pointerId, row };
    try {
      row.setPointerCapture(e.pointerId);
    } catch {
      /* ignore */
    }
    setHighlight(row);
  }

  function onMenuPointerMove(e) {
    if (!session || session.mode !== 'menu-drag' || session.pointerId !== e.pointerId) return;
    setHighlight(hitRow(e.clientX, e.clientY));
  }

  function onMenuPointerUp(e) {
    if (!session || session.mode !== 'menu-drag' || session.pointerId !== e.pointerId) return;
    releaseCapture(session.row, e.pointerId);
    const row = hitRow(e.clientX, e.clientY);
    if (row && menu.contains(row)) perform(row);
    else close(null);
    session = null;
  }

  function onMenuPointerCancel(e) {
    if (!session || session.mode !== 'menu-drag' || session.pointerId !== e.pointerId) return;
    releaseCapture(session.row, e.pointerId);
    session = null;
    setHighlight(null);
  }

  function onDocPointerDown(e) {
    if (!isOpen()) return;
    if (menu.contains(e.target) || anchor.contains(e.target)) return;
    close(null);
  }

  anchor.addEventListener('click', onAnchorClick);
  anchor.addEventListener('pointerdown', onPointerDown);
  anchor.addEventListener('pointermove', onPointerMove);
  anchor.addEventListener('pointerup', finishAnchorPointer);
  anchor.addEventListener('pointercancel', finishAnchorPointer);
  menu.addEventListener('click', onMenuClick);
  menu.addEventListener('pointerdown', onMenuPointerDown);
  menu.addEventListener('pointermove', onMenuPointerMove);
  menu.addEventListener('pointerup', onMenuPointerUp);
  menu.addEventListener('pointercancel', onMenuPointerCancel);
  document.addEventListener('pointerdown', onDocPointerDown, true);

  const destroy = () => {
    clearLongPress();
    anchor.removeEventListener('click', onAnchorClick);
    anchor.removeEventListener('pointerdown', onPointerDown);
    anchor.removeEventListener('pointermove', onPointerMove);
    anchor.removeEventListener('pointerup', finishAnchorPointer);
    anchor.removeEventListener('pointercancel', finishAnchorPointer);
    menu.removeEventListener('click', onMenuClick);
    menu.removeEventListener('pointerdown', onMenuPointerDown);
    menu.removeEventListener('pointermove', onMenuPointerMove);
    menu.removeEventListener('pointerup', onMenuPointerUp);
    menu.removeEventListener('pointercancel', onMenuPointerCancel);
    document.removeEventListener('pointerdown', onDocPointerDown, true);
  };

  return { destroy };
}
