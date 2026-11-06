/* Hà Lệ Quyên: ô chọn tệp ở màn rút ký gửi: hiện tên tệp, báo lỗi sai định dạng hoặc quá dung lượng ngay trên trình duyệt. */
(() => {
  'use strict';
  const MAX_BYTES = 10 * 1024 * 1024;
  const ALLOWED = /\.(jpe?g|png|pdf)$/i;

  const zone = document.querySelector('[data-withdrawn-file]');
  if (!zone) return;
  const input = zone.querySelector('input[type="file"]');
  const fileName = zone.querySelector('[data-file-name]');
  const clear = zone.querySelector('[data-file-clear]');
  const error = zone.parentElement.querySelector('[data-file-error]');
  const emptyText = fileName.textContent;

  function reset(message) {
    input.value = '';
    fileName.textContent = emptyText;
    zone.classList.remove('has-file');
    clear.hidden = true;
    error.textContent = message;
    error.hidden = !message;
  }

  input.addEventListener('change', () => {
    const file = input.files[0];
    if (!file) return reset('');
    // Kéo thả bỏ qua thuộc tính accept nên phải tự kiểm tra định dạng
    if (!ALLOWED.test(file.name)) return reset('Chỉ nhận tệp JPG, PNG hoặc PDF.');
    if (file.size > MAX_BYTES) return reset('Tệp vượt quá 10MB.');
    fileName.textContent = file.name;
    zone.classList.add('has-file');
    clear.hidden = false;
    error.hidden = true;
  });

  clear.addEventListener('click', () => {
    reset('');
    input.focus();
  });

  ['dragenter', 'dragover'].forEach((type) => zone.addEventListener(type, () => zone.classList.add('is-dragging')));
  ['dragleave', 'drop'].forEach((type) => zone.addEventListener(type, () => zone.classList.remove('is-dragging')));
})();
