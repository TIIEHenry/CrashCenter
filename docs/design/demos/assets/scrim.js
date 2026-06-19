/** Scroll-linked top/bottom scrim — returns single update fn */
export function initScrollScrim({
  scrollEl,
  topScrim,
  bottomFade,
  topFgEls = [],
  topTitleEl = null,
}) {
  const root = document.documentElement;
  const fadeDistance = () =>
    parseFloat(getComputedStyle(root).getPropertyValue('--scrim-fade-distance')) || 32;
  const bottomFadeHeight = () =>
    parseFloat(getComputedStyle(root).getPropertyValue('--scrim-bottom-fade-height')) || 96;

  let fgIsLight = null;

  const setAdaptiveFg = (useLight) => {
    if (fgIsLight === useLight) return;
    fgIsLight = useLight;
    const color = useLight ? 'var(--fg-on-scrim-light)' : 'var(--fg-on-scrim-dark)';
    topFgEls.forEach((el) => {
      el.style.color = color;
    });
  };

  const update = () => {
    const scrollTop = scrollEl.scrollTop;
    const scrollHeight = scrollEl.scrollHeight;
    const clientHeight = scrollEl.clientHeight;
    const maxScroll = Math.max(0, scrollHeight - clientHeight);
    const fade = fadeDistance();

    const topScrimHeight = () => {
      if (!topScrim) return 84;
      return parseFloat(getComputedStyle(topScrim).height) || 84;
    };

    const topOverlap = Math.min(scrollTop, topScrimHeight());
    const topAlpha = maxScroll > 0 ? Math.min(1, topOverlap / fade) : 0;
    topScrim.style.opacity = String(topAlpha);

    const remaining = maxScroll - scrollTop;
    const bottomOverlap = Math.min(remaining, bottomFadeHeight());
    const bottomAlpha =
      maxScroll > 40 && remaining > 4 ? Math.min(1, bottomOverlap / fade) : 0;
    bottomFade.style.opacity = String(bottomAlpha);

    // Compact pinned title only when content scrolls under top scrim (resting = large hero only).
    if (topTitleEl) {
      const showCompact = topAlpha > 0.12;
      topTitleEl.classList.toggle('is-compact-visible', showCompact);
    }

    const isLightFeed = Boolean(scrollEl.closest('.light-feed'));
    if (isLightFeed) {
      setAdaptiveFg(false);
      return;
    }

    const useLightFg = topAlpha > 0.2;
    setAdaptiveFg(useLightFg);
  };

  scrollEl.addEventListener('scroll', update, { passive: true });
  const ro = new ResizeObserver(update);
  ro.observe(scrollEl);
  update();
  return update;
}
