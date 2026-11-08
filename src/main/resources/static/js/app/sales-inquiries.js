/* Đỗ Lan Anh: Luồng Tư vấn & lịch xem xe: bấm dòng để xem nhanh, giữ dòng đang chọn trong khung bảng, hộp thoại đóng tư vấn. */
(() => {
  'use strict';

  const list = document.getElementById('inquiry-list');
  if (list) {
    list.querySelector('tr.is-selected')?.scrollIntoView({ block: 'nearest' });
    list.querySelectorAll('tbody tr').forEach((row) => row.addEventListener('click', (event) => {
      if (event.target.closest('a, button, summary, details, form')) return;
      const select = row.querySelector('[data-select]');
      if (select) select.click();
    }));

    // Bỏ query rỗng khi submit lọc → URL ngắn gọn
    list.querySelector('.inq-toolbar')?.addEventListener('submit', (event) => {
      const form = event.currentTarget;
      Array.from(form.elements).forEach((el) => {
        if (!el.name || el.disabled || el.type === 'submit' || el.type === 'button') return;
        if (el.name === 'size' && String(el.value) === '10') el.disabled = true;
        else if (el.name === 'page' && String(el.value) === '0') el.disabled = true;
        else if (el.value == null || String(el.value).trim() === '') el.disabled = true;
      });
    });
  }

  const detail = document.getElementById('inquiry-detail');
  const dialog = detail?.querySelector('#inq-close-dialog');
  if (dialog) {
    detail.querySelector('[data-open-close-dialog]')?.addEventListener('click', () => dialog.showModal());
    dialog.querySelector('[data-close-dialog]').addEventListener('click', () => dialog.close());
    if (detail.dataset.openClose === 'true') dialog.showModal();
  }
})();
