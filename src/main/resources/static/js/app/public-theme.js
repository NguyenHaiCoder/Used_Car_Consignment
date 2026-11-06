/* Public pages share a theme preference; storage may be disabled in private browsers. */
(() => {
  const button = document.getElementById('themeToggle');
  if (!button) return;
  let theme = 'light';
  try { theme = localStorage.getItem('theme') === 'dark' ? 'dark' : 'light'; } catch (_) { /* use light */ }
  function apply() {
    const dark = theme === 'dark';
    document.body.dataset.theme = theme;
    button.innerHTML = `<i class="fa-solid fa-${dark ? 'moon' : 'sun'}" aria-hidden="true"></i>`;
    button.setAttribute('aria-label', dark ? 'Chuyển giao diện sáng' : 'Chuyển giao diện tối');
    button.setAttribute('aria-pressed', String(dark));
  }
  button.addEventListener('click', () => {
    theme = theme === 'dark' ? 'light' : 'dark';
    try { localStorage.setItem('theme', theme); } catch (_) { /* preference is kept for this page */ }
    apply();
  });
  apply();
})();
