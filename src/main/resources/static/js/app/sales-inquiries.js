/* Đỗ Lan Anh: Luồng Tư vấn & lịch xem xe: bấm dòng để xem nhanh, giữ dòng đang chọn trong khung bảng, hộp thoại đóng tư vấn. */
(() => {
  'use strict';

  const list = document.getElementById('inquiry-list');
  if (list) {
    list.querySelector('tr.is-selected')?.scrollIntoView({ block: 'nearest' });
    list.querySelectorAll('tbody tr').forEach((row) => row.addEventListener('click', (event) => {
      if (event.target.closest('a, button, summary, details, form')) return;
      row.querySelector('[data-select]').click();
    }));
  }

  const detail = document.getElementById('inquiry-detail');
  const dialog = detail?.querySelector('#inq-close-dialog');
  if (dialog) {
    detail.querySelector('[data-open-close-dialog]')?.addEventListener('click', () => dialog.showModal());
    dialog.querySelector('[data-close-dialog]').addEventListener('click', () => dialog.close());
    if (detail.dataset.openClose === 'true') dialog.showModal();
  }
})();
