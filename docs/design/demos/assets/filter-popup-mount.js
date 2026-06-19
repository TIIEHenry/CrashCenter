/** Mount filter popup on an anchor; re-call after anchor DOM is replaced. */
import { initPressDragMenu } from './press-drag-menu.js';

export function mountFilterPopup({
  host,
  menu,
  getAnchor,
  onSelect,
  statusEl,
  placement = 'above',
  onBeforeOpen,
}) {
  let teardown = null;

  const positionMenu = () => {
    const anchor = getAnchor();
    if (!anchor || !host) return;
    const hostRect = host.getBoundingClientRect();
    const anchorRect = anchor.getBoundingClientRect();
    const centerX = anchorRect.left - hostRect.left + anchorRect.width / 2;
    menu.style.left = `${centerX}px`;
    menu.style.transform = 'translateX(-50%)';
    menu.style.right = 'auto';

    if (placement === 'below') {
      menu.style.top = `${anchorRect.bottom - hostRect.top + 8}px`;
      menu.style.bottom = 'auto';
    } else {
      menu.style.bottom = `${hostRect.bottom - anchorRect.top + 8}px`;
      menu.style.top = 'auto';
    }
  };

  const bind = () => {
    teardown?.();
    const anchor = getAnchor();
    if (!anchor) return;

    const menuBinding = initPressDragMenu({
      anchor,
      menu,
      statusEl,
      onSelect,
      onMenuOpen: () => {
        onBeforeOpen?.();
        positionMenu();
      },
    });

    const onOpen = () => positionMenu();
    anchor.addEventListener('click', onOpen);

    const scrollEl = host.querySelector('.feed-scroll');
    const onScroll = () => {
      if (!menu.hidden) {
        menu.hidden = true;
        menu.setAttribute('hidden', '');
      }
    };
    scrollEl?.addEventListener('scroll', onScroll, { passive: true });

    teardown = () => {
      anchor.removeEventListener('click', onOpen);
      scrollEl?.removeEventListener('scroll', onScroll);
      menuBinding?.destroy?.();
    };
  };

  bind();
  return {
    refresh: bind,
    destroy: () => teardown?.(),
  };
}

/** Close other menus when one opens (shared menu element). */
export function closeMenu(menu) {
  if (!menu) return;
  menu.hidden = true;
  menu.setAttribute('hidden', '');
}
