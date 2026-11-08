/* Hà Lệ Quyên: form tạo/sửa hồ sơ ký gửi — đồng bộ hãng theo dòng xe, ngày ↔ số ngày hạn ký gửi, xóa ảnh. */
(() => {
  'use strict';

  const root = document.getElementById('create-consignment')
      || document.getElementById('edit-consignment');
  if (!root) return;

  function syncBrandFromModel() {
    const modelSelect = root.querySelector('#vehicle-model');
    if (!modelSelect) return;
    const targetSel = modelSelect.getAttribute('data-brand-target') || '#vehicle-brand';
    const brandInput = root.querySelector(targetSel);
    if (!brandInput) return;
    const option = modelSelect.options[modelSelect.selectedIndex];
    brandInput.value = option && option.value ? (option.getAttribute('data-brand') || '') : '';
  }

  const modelSelect = root.querySelector('#vehicle-model');
  if (modelSelect) {
    modelSelect.addEventListener('change', () => {
      syncBrandFromModel();
    });
    syncBrandFromModel();
  }

  function parseIsoDate(value) {
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
    const [y, m, d] = value.split('-').map(Number);
    return new Date(y, m - 1, d);
  }

  function toIsoDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  function isoToVi(iso) {
    if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) return '';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  function inclusiveDays(from, to) {
    const ms = to.getTime() - from.getTime();
    if (Number.isNaN(ms) || ms < 0) return null;
    return Math.floor(ms / 86400000) + 1;
  }

  function syncTermDaysFromRange(wrap) {
    const fromEl = wrap.querySelector('[data-term-from]');
    const toEl = wrap.querySelector('[data-term-to]');
    const daysEl = wrap.querySelector('[data-term-days-input]');
    if (!fromEl || !toEl || !daysEl) return;
    const from = parseIsoDate(fromEl.value);
    const to = parseIsoDate(toEl.value);
    if (!from || !to) return;
    const days = inclusiveDays(from, to);
    if (days != null) daysEl.value = String(days);
  }

  function syncTermToFromDays(wrap) {
    const fromEl = wrap.querySelector('[data-term-from]');
    const toEl = wrap.querySelector('[data-term-to]');
    const daysEl = wrap.querySelector('[data-term-days-input]');
    if (!fromEl || !toEl || !daysEl) return;
    const from = parseIsoDate(fromEl.value);
    const days = Number(daysEl.value);
    if (!from || !Number.isFinite(days) || days < 1) return;
    const to = new Date(from.getFullYear(), from.getMonth(), from.getDate() + (days - 1));
    const iso = toIsoDate(to);
    toEl.value = iso;
    const display = toEl.closest('.date-range-field')?.querySelector('.date-range-display');
    if (display) display.value = isoToVi(iso);
  }

  root.querySelectorAll('[data-term-days]').forEach((wrap) => {
    syncTermDaysFromRange(wrap);
    wrap.querySelectorAll('[data-term-from], [data-term-to]').forEach((el) => {
      el.addEventListener('change', () => {
        syncTermDaysFromRange(wrap);
      });
    });
    wrap.querySelector('[data-term-days-input]')?.addEventListener('input', () => {
      syncTermToFromDays(wrap);
    });
  });

  root.addEventListener('click', (event) => {
    const remove = event.target.closest('.photo-remove');
    if (!remove || !root.contains(remove)) return;
    const slot = remove.closest('.photo-slot');
    if (slot) slot.remove();
  });

})();
