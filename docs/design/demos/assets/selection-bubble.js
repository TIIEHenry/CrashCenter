/**
 * Anchors .selection-bubble to document Selection rects.
 */
export function initSelectionBubble(options) {
  const {
    host,
    root = document,
    anchorLayer,
    onAction,
    onSubmit,
    flipBelow = true,
  } = options;

  const layer = anchorLayer || root;
  let suppressClose = false;

  function hide() {
    host.hidden = true;
    root.querySelectorAll('.selection-handle-demo').forEach((el) => {
      el.hidden = true;
    });
  }

  function positionBubble(rect) {
    const layerRect = layer.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2 - layerRect.left;
    const topY = rect.top - layerRect.top;
    const bottomY = rect.bottom - layerRect.top;

    host.style.left = `${centerX}px`;
    host.classList.remove('is-below');

    const hostHeight = host.offsetHeight || 44;
    const gap = 8;
    if (flipBelow && topY < hostHeight + gap + 8) {
      host.style.top = `${bottomY}px`;
      host.classList.add('is-below');
    } else {
      host.style.top = `${topY}px`;
    }
  }

  function updateFromSelection() {
    const sel = root.getSelection?.();
    if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
      if (!suppressClose) hide();
      return;
    }

    const range = sel.getRangeAt(0);
    if (!layer.contains(range.commonAncestorContainer)) {
      hide();
      return;
    }

    const rect = range.getBoundingClientRect();
    if (!rect.width && !rect.height) {
      hide();
      return;
    }

    host.hidden = false;
    positionBubble(rect);

    const startHandle = root.getElementById('sel-handle-start');
    const endHandle = root.getElementById('sel-handle-end');
    const layerRect = layer.getBoundingClientRect();
    if (startHandle && endHandle) {
      startHandle.hidden = false;
      endHandle.hidden = false;
      startHandle.style.left = `${rect.left - layerRect.left}px`;
      startHandle.style.top = `${rect.bottom - layerRect.top}px`;
      endHandle.style.left = `${rect.right - layerRect.left}px`;
      endHandle.style.top = `${rect.bottom - layerRect.top}px`;
    }
  }

  root.addEventListener('selectionchange', updateFromSelection);

  root.addEventListener('mousedown', (e) => {
    if (host.contains(e.target)) {
      suppressClose = true;
    }
  });

  root.addEventListener('mouseup', () => {
    suppressClose = false;
  });

  root.addEventListener('pointerdown', (e) => {
    if (host.hidden) return;
    if (!host.contains(e.target) && !layer.contains(e.target)) {
      hide();
      root.getSelection()?.removeAllRanges();
    }
  });

  host.querySelectorAll('[data-bubble-action]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const label = btn.dataset.bubbleAction || btn.textContent?.trim();
      onAction?.(btn.dataset.actionValue || label, label, btn);
    });
  });

  const form = host.querySelector('.selection-bubble-input-slot');
  const input = form?.querySelector('input');
  const submit = form?.querySelector('.selection-bubble-input-submit');

  const doSubmit = () => {
    const text = input?.value?.trim();
    if (!text) return;
    onSubmit?.(text);
    if (input) input.value = '';
  };

  submit?.addEventListener('click', (e) => {
    e.preventDefault();
    doSubmit();
  });

  input?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      doSubmit();
    }
  });

  return { hide, updateFromSelection };
}
