(() => {
  const form = document.getElementById('reservation-create-form')
    || document.getElementById('reservation-edit-form');
  const deposit = document.getElementById('deposit');
  if (!form || !deposit) return;

  const digits = value => value.replace(/\D/g, '');
  const format = value => digits(value).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  const caretAfter = (value, count, skipSeparator = true) => {
    if (count === 0) return 0;
    let seen = 0;
    for (let index = 0; index < value.length; index++) {
      if (/\d/.test(value[index])) seen++;
      if (seen === count) {
        const next = index + 1;
        return skipSeparator && value[next] === '.' ? next + 1 : next;
      }
    }
    return value.length;
  };

  // A persisted BigDecimal can render as 20000000.00; those cents are not VND digits.
  const initial = /^\d+\.00$/.test(deposit.value) ? deposit.value.slice(0, -3) : deposit.value;
  deposit.value = format(initial);
  let previousValue = deposit.value;

  const update = event => {
    if (event.isComposing) return;
    const raw = deposit.value;
    const cursor = deposit.selectionStart ?? raw.length;
    const count = digits(raw.slice(0, cursor)).length;
    const formatted = format(raw);
    const deletedOnlySeparator = event.inputType?.startsWith('delete')
      && digits(raw) === digits(previousValue) && raw.length < previousValue.length;
    deposit.value = formatted;
    const next = caretAfter(formatted, count,
      !(deletedOnlySeparator && event.inputType === 'deleteContentBackward'));
    deposit.setSelectionRange(next, next);
    previousValue = formatted;
  };

  deposit.addEventListener('input', update);
  deposit.addEventListener('compositionend', update);
  form.addEventListener('submit', () => {
    deposit.value = digits(deposit.value);
  });
})();

(() => {
  const picker = document.getElementById('inquiry');
  const card = document.getElementById('reservation-eligibility');
  if (!picker || !card) return;

  const createForm = document.getElementById('reservation-create-form');
  const createButton = createForm?.querySelector('.reservation-submit');
  const selectedInquiryId = createForm?.querySelector('[name="inquiryId"]')?.value ?? '';
  const selectionNote = document.getElementById('reservation-selection-note');
  const summary = document.getElementById('reservation-checks-summary');
  const checks = {
    vehicle: {
      row: document.getElementById('reservation-vehicle-check'),
      state: document.getElementById('reservation-vehicle-state'),
      detail: document.getElementById('reservation-vehicle-detail')
    },
    active: {
      row: document.getElementById('reservation-active-check'),
      state: document.getElementById('reservation-active-state'),
      detail: document.getElementById('reservation-active-detail')
    }
  };
  let pending;

  const syncSelection = () => {
    if (!createButton) return;
    const changed = picker.value !== selectedInquiryId;
    createButton.disabled = changed;
    if (selectionNote) selectionNote.hidden = !changed;
  };
  syncSelection();

  const show = (check, status, label, detail) => {
    check.row.dataset.state = status;
    check.row.querySelector('.reservation-check-icon').textContent =
      status === 'success' ? '✓' : status === 'empty' ? '–' : status === 'loading' ? '…' : '!';
    check.state.textContent = label;
    check.detail.textContent = detail;
  };

  const showBoth = (status, label, detail, heading) => {
    summary.textContent = heading;
    show(checks.vehicle, status, label, detail);
    show(checks.active, status, label, detail);
  };

  picker.addEventListener('change', async () => {
    syncSelection();
    pending?.abort();
    const inquiryId = picker.value;
    if (!inquiryId) {
      showBoth('empty', 'Chưa chọn xe', 'Chọn phiếu tư vấn để kiểm tra xe.', 'Chưa kiểm tra');
      return;
    }

    const request = new AbortController();
    pending = request;
    showBoth('loading', 'Đang kiểm tra', 'Đang lấy điều kiện của xe đã chọn.', 'Đang kiểm tra');
    try {
      const url = new URL(card.dataset.checkUrl, window.location.origin);
      url.searchParams.set('inquiryId', inquiryId);
      const response = await fetch(url, { headers: { Accept: 'application/json' }, cache: 'no-store', signal: request.signal });
      if (!response.ok) throw new Error('Eligibility request failed');
      const result = await response.json();
      if (request.signal.aborted || picker.value !== inquiryId) return;
      if (result.inquiryId !== Number(inquiryId)
          || typeof result.vehicleEligible !== 'boolean'
          || typeof result.noActiveReservation !== 'boolean'
          || typeof result.vehicleStatus !== 'string') throw new Error('Invalid eligibility response');

      summary.textContent = 'Đã kiểm tra';
      show(checks.vehicle, result.vehicleEligible ? 'success' : 'failure',
        result.vehicleEligible ? 'Đủ điều kiện' : 'Chưa đủ điều kiện',
        `Trạng thái xe: ${result.vehicleStatus}${result.vehicleReason ? ` — ${result.vehicleReason}` : ''}`);
      show(checks.active, result.noActiveReservation ? 'success' : 'failure',
        result.noActiveReservation ? 'Đủ điều kiện' : 'Chưa đủ điều kiện',
        result.noActiveReservation ? 'Xe chưa có đơn giữ xe đang hoạt động' : 'Xe đã có đơn giữ xe đang hoạt động');
    } catch (error) {
      if (!request.signal.aborted) {
        showBoth('error', 'Không lấy được kết quả', 'Thử chọn lại phiếu tư vấn.', 'Lỗi kiểm tra');
      }
    }
  });
})();
