/* Định dạng giá chào bán khi kiểm định: 1000000 → 1.000.000, lúc gửi thì bỏ dấu chấm. */
(() => {
  'use strict';

  const digits = (text) => String(text).replace(/\D/g, '');
  const money = (value) => new Intl.NumberFormat('vi-VN').format(value);
  const input = document.querySelector('#inspector-inspect [data-money]');
  if (!input) return;

  const paint = () => {
    const clean = digits(input.value);
    input.value = clean ? money(Number(clean)) : '';
  };

  paint();
  input.addEventListener('input', paint);
  input.form?.addEventListener('submit', () => {
    input.value = digits(input.value);
  });
})();
