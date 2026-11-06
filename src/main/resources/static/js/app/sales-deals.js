/* Đỗ Lan Anh: màn Đặt cọc giữ xe và Hợp đồng bán xe: chọn xong tự tải lại, định dạng tiền, hộp thoại hủy cọc, gửi chữ ký. */
(() => {
  'use strict';

  const digits = (text) => String(text).replace(/\D/g, '');
  const money = (value) => new Intl.NumberFormat('vi-VN').format(value);

  document.querySelectorAll('[data-auto-submit]').forEach((select) => {
    select.addEventListener('change', () => {
      if (select.value) select.form.submit();
    });
  });

  // Bản nháp đang gõ: giữ dấu chấm ngăn cách nghìn và cập nhật ngay số tiền còn lại.
  const summary = document.querySelector('[data-deal-price]');
  const contractRow = document.querySelector('[data-deposit]');

  const refresh = (input) => {
    const value = Number(digits(input.value));
    if (summary) {
      const price = Number(summary.dataset.dealPrice);
      summary.querySelector('[data-deal-deposit]').textContent = `${money(value)} ₫`;
      summary.querySelector('[data-deal-remaining]').textContent = `${money(Math.max(price - value, 0))} ₫`;
    }
    if (contractRow) {
      const left = value - Number(contractRow.dataset.deposit);
      contractRow.querySelector('[data-contract-remaining]').value = money(Math.max(left, 0));
    }
  };

  document.querySelectorAll('[data-money]').forEach((input) => {
    refresh(input);
    if (input.readOnly) return;
    input.addEventListener('input', () => {
      const clean = digits(input.value);
      input.value = clean ? money(Number(clean)) : '';
      refresh(input);
    });
  });

  const detail = document.getElementById('hold-detail');
  const dialog = detail?.querySelector('#hold-dialog');
  if (dialog) {
    detail.querySelector('button[data-open-dialog]').addEventListener('click', () => dialog.showModal());
    dialog.querySelector('[data-close-dialog]').addEventListener('click', () => dialog.close());
    if (detail.dataset.openDialog === 'true') dialog.showModal();
  }

  document.querySelector('[data-print]')?.addEventListener('click', () => window.print());

  // consignment-contracts.js chỉ vẽ chữ ký lên canvas, nên lúc gửi form mới chép ảnh vào ô ẩn.
  document.getElementById('contract-form')?.addEventListener('submit', (event) => {
    event.currentTarget.querySelectorAll('[data-sig-input]').forEach((input) => {
      const pad = event.currentTarget.querySelector(`.sig-pad[data-sig="${input.dataset.sigInput}"]`);
      input.value = pad.classList.contains('has-ink') ? pad.querySelector('canvas').toDataURL('image/png') : '';
    });
  });
})();
