/* Nguyễn Vũ Trường An: thêm / xoá / đánh số lại bước loại kiểm định.
   Server bind theo chỉ mục steps[i].title / steps[i].hint nên chỉ mục phải liên tục 0..N-1. */
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
      var title = row.querySelector('[data-step-title]');
      var hint = row.querySelector('[data-step-hint]');
      if (title) {
        title.name = 'steps[' + index + '].title';
        title.id = 'steps' + index + '.title';
      }
      if (hint) {
        hint.name = 'steps[' + index + '].hint';
        hint.id = 'steps' + index + '.hint';
      }
    });
  }

  function bindRemove(row) {
    var btn = row.querySelector('[data-remove-step]');
    if (!btn) return;
    btn.addEventListener('click', function () {
      if (list.querySelectorAll('[data-step-row]').length <= 1) {
        var title = row.querySelector('[data-step-title]');
        var hint = row.querySelector('[data-step-hint]');
        if (title) title.value = '';
        if (hint) hint.value = '';
        return;
      }
      row.remove();
      renumber();
    });
  }

  function createRow() {
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
          '<input type="text" data-step-title maxlength="160" placeholder="Vd: Ngoại thất, giấy tờ, động cơ...">' +
        '</div>' +
        '<div class="field">' +
          '<label>Mô tả / gợi ý</label>' +
          '<input type="text" data-step-hint maxlength="500" placeholder="Chi tiết cần kiểm tra (tuỳ chọn)">' +
        '</div>' +
      '</div>';
    bindRemove(row);
    return row;
  }

  list.querySelectorAll('[data-step-row]').forEach(bindRemove);

  addBtn.addEventListener('click', function () {
    list.appendChild(createRow());
    renumber();
    var last = list.querySelector('[data-step-row]:last-child [data-step-title]');
    if (last) last.focus();
  });

  renumber();
})();
