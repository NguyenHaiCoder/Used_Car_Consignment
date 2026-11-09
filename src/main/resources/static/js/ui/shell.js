(() => {
  'use strict';
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

  let toastTimer;
  function toast(message) {
    const box = $('#toast');
    if (!box) return;
    box.textContent = message;
    box.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { box.hidden = true; }, 5000);
  }

  const flashNotice = $('.screen-content > .alert');
  if (flashNotice && flashNotice.textContent.trim()) {
    toast(flashNotice.textContent.trim());
  }

  const sidebar = $('#app-sidebar');
  const menu = $('.menu-toggle');
  const overlay = $('.nav-overlay');
  let previousFocus;

  function setMenu(open) {
    if (!sidebar) return;
    sidebar.classList.toggle('is-open', open);
    if (menu) menu.setAttribute('aria-expanded', String(open));
    if (overlay) overlay.hidden = !open;
    document.body.style.overflow = open ? 'hidden' : '';
    if (open) {
      previousFocus = document.activeElement;
      $('.sidebar-close')?.focus();
    } else if (previousFocus) {
      previousFocus.focus();
    }
  }

  menu?.addEventListener('click', () => setMenu(!sidebar.classList.contains('is-open')));
  $('.sidebar-close')?.addEventListener('click', () => setMenu(false));
  overlay?.addEventListener('click', () => setMenu(false));

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      setMenu(false);
      $$('details.user-menu[open], details.filter-dropdown[open], details.select-dd[open]').forEach((x) => { x.open = false; });
    }
    if (e.key === 'Tab' && sidebar?.classList.contains('is-open')) {
      const focusable = $$('a, button, summary', sidebar).filter((x) => x.getClientRects().length);
      if (!focusable.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      }
      if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    }
  });

  window.addEventListener('resize', () => {
    if (window.innerWidth > 960 && sidebar?.classList.contains('is-open')) setMenu(false);
  });

  document.addEventListener('click', (e) => {
    $$('details.user-menu[open], details.filter-dropdown[open], details.select-dd[open]').forEach((x) => {
      if (!x.contains(e.target)) x.open = false;
    });
  });

  $$('.nav-group').forEach((group) => {
    if ($('.nav-link.active', group)) group.open = true;
  });

  const rowMenus = $$('.records .row-menu');
  rowMenus.forEach((details) => {
    const trigger = $('summary', details);
    const panel = $('.menu-pop', details);
    if (!trigger || !panel) return;
    const usePopover = typeof panel.showPopover === 'function';
    if (usePopover) panel.setAttribute('popover', 'manual');
    trigger.setAttribute('aria-expanded', String(details.open));

    details.addEventListener('toggle', () => {
      trigger.setAttribute('aria-expanded', String(details.open));
      if (!details.open) {
        if (usePopover && panel.matches(':popover-open')) panel.hidePopover();
        return;
      }
      rowMenus.forEach((other) => { if (other !== details) other.open = false; });
      const bounds = trigger.getBoundingClientRect();
      const top = bounds.bottom + 6;
      panel.style.top = `${top}px`;
      panel.style.maxHeight = `${Math.max(0, window.innerHeight - top - 8)}px`;
      if (usePopover && !panel.matches(':popover-open')) panel.showPopover();
      const width = panel.getBoundingClientRect().width;
      panel.style.left = `${Math.max(8, Math.min(bounds.right - width, window.innerWidth - width - 8))}px`;
    });

    details.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && details.open) trigger.focus();
    });
  });

  function closeRowMenus(event) {
    rowMenus.forEach((details) => {
      // Scrolling inside a long menu should leave it open.
      if (details.open && !$('.menu-pop', details)?.contains(event.target)) details.open = false;
    });
  }
  document.addEventListener('scroll', closeRowMenus, true);
  window.addEventListener('resize', closeRowMenus);

  /* Khoảng ngày: ô hiện dd/mm/yyyy, input[type=date] giữ ISO để submit */
  function isoToVi(iso) {
    if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) return '';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }
  function openDatePicker(native) {
    if (!native) return;
    try {
      if (typeof native.showPicker === 'function') {
        native.showPicker();
        return;
      }
    } catch (_) { /* fallback below */ }
    native.focus();
    native.click();
  }

  $$('[data-date-range]').forEach((range) => {
    const form = range.closest('form');
    const fields = $$('.date-range-field', range);
    const natives = fields.map((field) => $('.date-range-native', field));

    range.querySelector(':scope > .ic')?.addEventListener('click', (e) => {
      e.preventDefault();
      openDatePicker(natives[0]);
    });

    fields.forEach((field) => {
      const display = $('.date-range-display', field);
      const native = $('.date-range-native', field);
      if (!display || !native) return;
      if (native.value) display.value = isoToVi(native.value);

      field.addEventListener('click', (e) => {
        if (e.target === native) return;
        e.preventDefault();
        openDatePicker(native);
      });

      native.addEventListener('change', () => {
        display.value = isoToVi(native.value);
        // Chỉ auto-submit bộ lọc GET trên màn list; form tạo/sửa không submit.
        if (form && String(form.method || '').toLowerCase() === 'get') form.requestSubmit();
      });

      native.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          openDatePicker(native);
        }
      });
    });
  });
})();
