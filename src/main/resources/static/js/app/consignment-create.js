/* Hà Lệ Quyên: checklist bắt buộc — đủ hết mới ✓ xanh; thiếu 1 trường → ✕ đỏ. */
(() => {
  'use strict';

  const root = document.getElementById('create-consignment')
      || document.getElementById('edit-consignment');
  if (!root) return;

  const ICON_DONE = '<path d="M20 6 9 17l-5-5"/>';
  const ICON_FAIL = '<path d="M18 6 6 18"/><path d="m6 6 12 12"/>';

  function requiredFields(section) {
    return Array.from(section.querySelectorAll('.field')).filter((field) =>
      field.querySelector('label .req')
    );
  }

  function fieldComplete(field) {
    const accessories = field.querySelector('.accessories-row');
    if (accessories) {
      return accessories.querySelectorAll('input[type="checkbox"]:checked').length > 0;
    }

    const photos = field.querySelector('.photo-grid');
    if (photos) {
      return photos.querySelectorAll('.photo-slot').length > 0;
    }

    const dateNatives = field.querySelectorAll('.date-range-native');
    if (dateNatives.length) {
      return Array.from(dateNatives).every((el) => String(el.value ?? '').trim() !== '');
    }

    const controls = field.querySelectorAll(
      'input:not([type="checkbox"]):not([type="hidden"]):not([readonly]):not(.date-range-display), select, textarea'
    );
    if (!controls.length) return true;
    return Array.from(controls).every((el) => String(el.value ?? '').trim() !== '');
  }

  function sectionComplete(key) {
    const section = root.querySelector(`[data-check-section="${key}"]`);
    if (!section) return false;
    const fields = requiredFields(section);
    if (!fields.length) return false;
    return fields.every(fieldComplete);
  }

  function setItemState(item, ok) {
    item.classList.toggle('is-done', ok);
    item.classList.toggle('is-fail', !ok);
    item.classList.remove('is-pending');
    const svg = item.querySelector('svg.ic');
    if (svg) svg.innerHTML = ok ? ICON_DONE : ICON_FAIL;
  }

  function refreshChecklist() {
    root.querySelectorAll('[data-create-checklist] [data-check]').forEach((item) => {
      const key = item.getAttribute('data-check');
      setItemState(item, sectionComplete(key));
    });
  }

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
      refreshChecklist();
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
        refreshChecklist();
      });
    });
    wrap.querySelector('[data-term-days-input]')?.addEventListener('input', () => {
      syncTermToFromDays(wrap);
      refreshChecklist();
    });
  });

  root.addEventListener('input', refreshChecklist);
  root.addEventListener('change', refreshChecklist);

  root.addEventListener('click', (event) => {
    const remove = event.target.closest('.photo-remove');
    if (!remove || !root.contains(remove)) return;
    const slot = remove.closest('.photo-slot');
    if (slot) {
      slot.remove();
      refreshChecklist();
    }
  });

  refreshChecklist();
})();
