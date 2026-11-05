(() => {
  const clearKey = document.currentScript?.dataset.clearDraft;
  if (clearKey) {
    try { localStorage.removeItem(clearKey); } catch { /* Storage may be disabled. */ }
  }
  const form = document.querySelector('#inquiry-editor .inquiry-form');
  if (!form) return;

  const fields = ['buyerId', 'vehicleId', 'channel', 'need', 'notes'];
  const buyer = form.elements.namedItem('buyerId');
  const vehicle = form.elements.namedItem('vehicleId');
  const loadVehicles = document.getElementById('inquiry-load-vehicles');
  const saveDraft = document.getElementById('inquiry-save-draft');
  const clearDraft = document.getElementById('inquiry-clear-draft');
  const message = document.getElementById('inquiry-draft-message');
  const key = form.dataset.draftKey;
  const showMessage = text => { message.textContent = text; message.hidden = false; };

  // Keep the existing authorized POST and CSRF token when refreshing eligible vehicles.
  loadVehicles.hidden = true;
  buyer.addEventListener('change', () => {
    vehicle.value = '';
    form.requestSubmit(loadVehicles);
  });
  saveDraft.hidden = false;
  clearDraft.hidden = false;
  saveDraft.addEventListener('click', () => {
    try {
      const draft = Object.fromEntries(fields.map(name => [name, form.elements.namedItem(name).value]));
      localStorage.setItem(key, JSON.stringify(draft));
      showMessage('Đã lưu nháp trên trình duyệt này.');
    } catch {
      showMessage('Trình duyệt không cho phép lưu nháp. Bạn vẫn có thể gửi biểu mẫu.');
    }
  });
  clearDraft.addEventListener('click', () => {
    try {
      localStorage.removeItem(key);
      if (key.startsWith('inquiry:create:')) {
        fields.forEach(name => { form.elements.namedItem(name).value = ''; });
      } else {
        form.reset();
      }
      showMessage('Đã xóa nháp.');
    } catch {
      showMessage('Không thể xóa nháp trong trình duyệt này.');
    }
  });

  // Never replace a buyer supplied by the calling page or values returned with errors.
  if (form.dataset.restoreDraft !== 'true' || form.dataset.hasErrors === 'true') return;
  try {
    const draft = JSON.parse(localStorage.getItem(key) || 'null');
    if (!draft || typeof draft !== 'object') return;
    fields.forEach(name => {
      if (typeof draft[name] !== 'string') return;
      const control = form.elements.namedItem(name);
      if (name === 'vehicleId' && draft[name] && !Array.from(vehicle.options).some(o => o.value === draft[name])) {
        vehicle.add(new Option('Xe đã lưu #' + draft[name], draft[name]));
      }
      control.value = draft[name];
    });
    showMessage('Đã khôi phục nháp trên trình duyệt.');
    if (buyer.value) form.requestSubmit(loadVehicles);
  } catch {
    showMessage('Không thể khôi phục nháp. Vui lòng nhập lại thông tin.');
  }
})();
