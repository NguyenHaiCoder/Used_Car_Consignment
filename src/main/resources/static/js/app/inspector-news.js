/* Nguyễn Vũ Trường An: màn Quản lý tin đăng: chọn nhiều dòng và thông báo demo của menu ba chấm. */
(() => {
  'use strict';
  const root = document.getElementById('news-list');
  if (!root) return;

  const checkAll = root.querySelector('[data-check-all]');
  const rows = Array.from(root.querySelectorAll('[data-check-row]'));

  checkAll?.addEventListener('change', () => {
    rows.forEach((row) => { row.checked = checkAll.checked; });
  });
  rows.forEach((row) => row.addEventListener('change', () => {
    const picked = rows.filter((x) => x.checked).length;
    checkAll.checked = picked === rows.length;
    checkAll.indeterminate = picked > 0 && picked < rows.length;
  }));

  // Dòng cuối không đủ chỗ phía dưới thì mở menu lên trên, thay vì để menu bị ép lùn rồi phải cuộn.
  root.querySelectorAll('.row-menu').forEach((details) => details.addEventListener('toggle', () => {
    if (!details.open) return;
    requestAnimationFrame(() => {
      const panel = details.querySelector('.menu-pop');
      if (panel.scrollHeight <= panel.clientHeight) return;
      panel.style.maxHeight = '';
      const top = details.querySelector('summary').getBoundingClientRect().top;
      panel.style.top = `${Math.max(8, top - panel.offsetHeight - 6)}px`;
    });
  }));

  root.addEventListener('click', (event) => {
    const button = event.target.closest('.menu-pop button');
    if (!button) return;
    button.closest('details').open = false;
  });
})();
