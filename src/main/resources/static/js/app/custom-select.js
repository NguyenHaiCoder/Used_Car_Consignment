(() => {
  'use strict';

  const CARET = `<span class="filter-dropdown-caret" aria-hidden="true"><svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg></span>`;

  function optionLabel(opt) {
    return (opt.textContent || '').trim() || opt.value || '—';
  }

  function placeMenu(details) {
    const menu = details.querySelector('[data-custom-select-menu]');
    const summary = details.querySelector('summary');
    if (!menu || !summary || !details.open) return;
    const r = summary.getBoundingClientRect();
    const maxH = Math.min(260, Math.max(120, window.innerHeight - r.bottom - 12));
    menu.style.position = 'fixed';
    menu.style.left = `${Math.max(8, r.left)}px`;
    menu.style.top = `${r.bottom + 4}px`;
    menu.style.width = `${r.width}px`;
    menu.style.maxHeight = `${maxH}px`;
    menu.style.zIndex = '400';
    menu.style.right = 'auto';
  }

  function clearMenu(details) {
    const menu = details.querySelector('[data-custom-select-menu]');
    if (!menu) return;
    menu.style.position = '';
    menu.style.left = '';
    menu.style.top = '';
    menu.style.width = '';
    menu.style.maxHeight = '';
    menu.style.zIndex = '';
    menu.style.right = '';
  }

  function buildMenu(select, menu, labelEl) {
    menu.replaceChildren();
    Array.from(select.options).forEach((opt, index) => {
      if (opt.hidden) return;
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'filter-dropdown-item';
      btn.setAttribute('role', 'option');
      btn.dataset.index = String(index);
      btn.textContent = optionLabel(opt);
      if (opt.disabled) {
        btn.disabled = true;
        btn.classList.add('is-disabled');
      }
      if (opt.selected) {
        btn.classList.add('is-active');
        btn.setAttribute('aria-selected', 'true');
      }
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        if (opt.disabled) return;
        select.selectedIndex = index;
        select.dispatchEvent(new Event('change', { bubbles: true }));
        select.dispatchEvent(new Event('input', { bubbles: true }));
        syncFromSelect(select);
        const details = menu.closest('details');
        if (details) details.open = false;
      });
      menu.appendChild(btn);
    });
    const selected = select.options[select.selectedIndex];
    labelEl.textContent = selected ? optionLabel(selected) : 'Chọn';
  }

  function syncFromSelect(select) {
    const root = select._customSelectRoot;
    if (!root) return;
    const labelEl = root.querySelector('[data-custom-select-label]');
    const menu = root.querySelector('[data-custom-select-menu]');
    if (!labelEl || !menu) return;
    buildMenu(select, menu, labelEl);
    root.classList.toggle('is-disabled', select.disabled);
    const summary = root.querySelector('summary');
    if (summary) {
      if (select.disabled) summary.setAttribute('aria-disabled', 'true');
      else summary.removeAttribute('aria-disabled');
    }
    if (root.open) placeMenu(root);
  }

  function enhance(select) {
    if (!select || select.dataset.customSelect === '1') return;
    if (select.multiple || select.size > 1) return;
    select.dataset.customSelect = '1';
    select.classList.add('select-native--hidden');

    const host = select.closest('span.select');
    if (host) host.classList.add('select--custom');

    const details = document.createElement('details');
    details.className = 'filter-dropdown select-dd';
    details.dataset.customSelect = '';
    select._customSelectRoot = details;
    if (select.disabled) details.classList.add('is-disabled');

    const summary = document.createElement('summary');
    summary.className = 'filter-dropdown-trigger select-dd-trigger';
    summary.setAttribute('aria-label', select.getAttribute('aria-label') || select.id || 'Chọn');

    const labelEl = document.createElement('span');
    labelEl.dataset.customSelectLabel = '';
    summary.appendChild(labelEl);
    summary.insertAdjacentHTML('beforeend', CARET);

    const menu = document.createElement('div');
    menu.className = 'filter-dropdown-menu';
    menu.setAttribute('role', 'listbox');
    menu.dataset.customSelectMenu = '';

    details.appendChild(summary);
    details.appendChild(menu);

    summary.addEventListener('click', (e) => {
      if (select.disabled) {
        e.preventDefault();
        details.open = false;
      }
    });

    details.addEventListener('toggle', () => {
      if (details.open) {
        document.querySelectorAll('details.select-dd[open]').forEach((other) => {
          if (other !== details) other.open = false;
        });
        placeMenu(details);
      } else {
        clearMenu(details);
      }
    });

    select.insertAdjacentElement('afterend', details);
    buildMenu(select, menu, labelEl);

    select.addEventListener('change', () => syncFromSelect(select));
    const mo = new MutationObserver(() => syncFromSelect(select));
    mo.observe(select, { childList: true, subtree: true, attributes: true });
  }

  function repositionOpen() {
    document.querySelectorAll('details.select-dd[open]').forEach(placeMenu);
  }

  window.addEventListener('resize', repositionOpen);
  document.addEventListener('scroll', repositionOpen, true);

  function boot() {
    document.querySelectorAll('span.select > select, select.field-select, .field-input > select')
      .forEach(enhance);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }

  window.ConsignCustomSelect = { enhance, refresh: boot };
})();
