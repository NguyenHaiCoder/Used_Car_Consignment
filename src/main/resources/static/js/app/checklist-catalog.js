/* Nguyễn Vũ Trường An: thêm / xoá / đánh số lại bước loại kiểm định (UI mock). */
(function () {
  var root = document.getElementById('steps-editor');
  if (!root) return;

  var list = root.querySelector('[data-steps-list]');
  var addBtn = root.querySelector('[data-add-step]');
  if (!list || !addBtn) return;

  function renumber() {
    var rows = list.querySelectorAll('[data-step-row]');
    rows.forEach(function (row, index) {
      var badge = row.querySelector('[data-step-order]');
      if (badge) badge.textContent = 'Bước ' + (index + 1);
    });
  }

  function bindRemove(row) {
    var btn = row.querySelector('[data-remove-step]');
    if (!btn) return;
    btn.addEventListener('click', function () {
      if (list.querySelectorAll('[data-step-row]').length <= 1) {
        var title = row.querySelector('input[name="stepTitles"]');
        var hint = row.querySelector('input[name="stepHints"]');
        if (title) title.value = '';
        if (hint) hint.value = '';
        return;
      }
      row.remove();
      renumber();
    });
  }

  function createRow(title, hint) {
    var row = document.createElement('div');
    row.className = 'step-row';
    row.setAttribute('data-step-row', '');
    row.innerHTML =
      '<div class="step-row-head">' +
        '<span class="step-order" data-step-order>Bước 1</span>' +
        '<button type="button" class="btn-c btn-c--ghost step-remove" data-remove-step>Xoá</button>' +
      '</div>' +
      '<div class="step-row-fields">' +
        '<div class="field">' +
          '<label>Kiểm định gì <span class="req">*</span></label>' +
          '<input type="text" name="stepTitles" required placeholder="Vd: Ngoại thất, giấy tờ, động cơ...">' +
        '</div>' +
        '<div class="field">' +
          '<label>Mô tả / gợi ý</label>' +
          '<input type="text" name="stepHints" placeholder="Chi tiết cần kiểm tra (tuỳ chọn)">' +
        '</div>' +
      '</div>';
    var titleInput = row.querySelector('input[name="stepTitles"]');
    var hintInput = row.querySelector('input[name="stepHints"]');
    if (titleInput && title) titleInput.value = title;
    if (hintInput && hint) hintInput.value = hint;
    bindRemove(row);
    return row;
  }

  list.querySelectorAll('[data-step-row]').forEach(bindRemove);

  addBtn.addEventListener('click', function () {
    list.appendChild(createRow('', ''));
    renumber();
    var last = list.querySelector('[data-step-row]:last-child input[name="stepTitles"]');
    if (last) last.focus();
  });

  renumber();
})();
