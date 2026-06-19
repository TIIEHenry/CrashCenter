/** ② Circular ripple — Toolbar 正圆 IconButton */
export function bindPressRipple(root = document) {
  root.querySelectorAll('.press-ripple').forEach((el) => {
    const down = () => el.classList.add('is-pressed');
    const up = () => el.classList.remove('is-pressed');
    el.addEventListener('pointerdown', down);
    el.addEventListener('pointerup', up);
    el.addEventListener('pointerleave', up);
    el.addEventListener('pointercancel', up);
  });
}

/** iOS-style press feedback (Paradigm ①) — overlay highlight, no scale */
export function bindPressIOS(root = document) {
  root.querySelectorAll('.press-ios').forEach((el) => {
    const down = () => el.classList.add('is-pressed');
    const up = () => el.classList.remove('is-pressed');
    el.addEventListener('pointerdown', down);
    el.addEventListener('pointerup', up);
    el.addEventListener('pointerleave', up);
    el.addEventListener('pointercancel', up);
    el.addEventListener('click', up);
  });
}

export function setTheme(theme) {
  document.documentElement.dataset.theme = theme;
  localStorage.setItem('clarence-demo-theme', theme);
}

export function initThemeToggle(btnId = 'theme-toggle') {
  const saved = localStorage.getItem('clarence-demo-theme') || 'light';
  setTheme(saved);
  const btn = document.getElementById(btnId);
  if (!btn) return;
  btn.addEventListener('click', () => {
    const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    btn.textContent = next === 'dark' ? '浅色' : '深色';
  });
  btn.textContent = saved === 'dark' ? '浅色' : '深色';
}
