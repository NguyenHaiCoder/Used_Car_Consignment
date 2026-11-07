/* Vũ Mạnh Cường: mở và đóng hộp thoại ở màn Thanh toán, Quyết toán. */
(() => {
  'use strict';

  document.querySelectorAll('[data-dialog]').forEach((button) => {
    const dialog = document.getElementById(button.dataset.dialog);
    button.addEventListener('click', () => dialog.showModal());
    dialog.querySelector('[data-close-dialog]').addEventListener('click', () => dialog.close());
  });
})();
