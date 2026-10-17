(() => {
  'use strict';
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));
  const kind = document.body.dataset.kind;
  const role = document.body.dataset.role;
  const normalize = text => text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLocaleLowerCase('vi');
  const money = amount => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  let toastTimer;
  function toast(message) {
    const box = $('#toast');
    if (!box) return;
    box.textContent = message; box.hidden = false;
    clearTimeout(toastTimer); toastTimer = setTimeout(() => { box.hidden = true; }, 5000);
  }

  // Responsive navigation closes on Escape, outside click and route navigation.
  const sidebar = $('#app-sidebar');
  const menu = $('.menu-toggle');
  const overlay = $('.nav-overlay');
  let previousFocus;
  function setMenu(open) {
    if (!sidebar) return;
    sidebar.classList.toggle('is-open', open);
    menu.setAttribute('aria-expanded', String(open)); overlay.hidden = !open;
    document.body.style.overflow = open ? 'hidden' : '';
    if (open) { previousFocus = document.activeElement; $('.sidebar-close').focus(); }
    else if (previousFocus) previousFocus.focus();
  }
  menu?.addEventListener('click', () => setMenu(!sidebar.classList.contains('is-open')));
  $('.sidebar-close')?.addEventListener('click', () => setMenu(false));
  overlay?.addEventListener('click', () => setMenu(false));
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') { setMenu(false); $$('details.user-menu[open]').forEach(x => x.open = false); }
    if (e.key === 'Tab' && sidebar?.classList.contains('is-open')) {
      const focusable = $$('a, button, summary', sidebar).filter(x => x.getClientRects().length);
      const first = focusable[0], last = focusable[focusable.length - 1];
      if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
      if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    }
  });
  window.addEventListener('resize', () => { if (window.innerWidth > 960 && sidebar?.classList.contains('is-open')) setMenu(false); });
  document.addEventListener('click', e => $$('details.user-menu[open]').forEach(x => { if (!x.contains(e.target)) x.open = false; }));
  $$('.nav-group').forEach(group => { if ($('.nav-link.active', group)) group.open = true; });

  // Review dialog is shared by forms and confirmation actions; all output uses textContent.
  const dialog = $('#review-dialog');
  let pendingConfirm = null;
  let dialogFocus;
  function review(title, pairs, callback) {
    if (!dialog) return;
    dialogFocus = document.activeElement;
    $('#dialog-title').textContent = title;
    const content = $('#review-content'); content.replaceChildren();
    const dl = document.createElement('dl');
    pairs.forEach(([label, value]) => {
      const block = document.createElement('div'); const dt = document.createElement('dt'); const dd = document.createElement('dd');
      dt.textContent = label; dd.textContent = value; block.append(dt, dd); dl.append(block);
    });
    content.append(dl); pendingConfirm = callback;
    $('#confirm-preview').disabled = false;
    dialog.showModal(); $('[data-close-dialog]', dialog).focus();
  }
  $$('[data-close-dialog]').forEach(button => button.addEventListener('click', () => dialog.close()));
  dialog?.addEventListener('close', () => { pendingConfirm = null; dialogFocus?.focus(); });
  $('#confirm-preview')?.addEventListener('click', () => {
    if (!pendingConfirm) return;
    const apply = pendingConfirm; pendingConfirm = null;
    $('#confirm-preview').disabled = true;
    apply(); dialog.close();
  });

  // Filter, locale-aware sorting and pagination operate on rendered read models only.
  const table = $('#records-table');
  let page = 1, sortIndex = -1, sortDirection = 1;
  const rows = table ? $$('tbody tr', table) : [];
  function filteredRows() {
    const text = normalize($('#table-search')?.value || ''); const status = $('#table-status')?.value;
    return rows.filter(row => normalize(row.textContent).includes(text) && (!status || row.dataset.status === status));
  }
  function renderTable() {
    if (!table) return;
    const selected = filteredRows(); const size = Number($('#table-size').value);
    if (sortIndex >= 0) selected.sort((a, b) => a.cells[sortIndex].textContent.localeCompare(b.cells[sortIndex].textContent, 'vi', {numeric:true}) * sortDirection);
    const pages = Math.max(1, Math.ceil(selected.length / size)); page = Math.min(page, pages);
    rows.forEach(row => { row.hidden = true; });
    selected.slice((page - 1) * size, page * size).forEach(row => { row.hidden = false; $('tbody',table).append(row); });
    $('#table-empty').hidden = selected.length !== 0;
    $('#table-count').textContent = selected.length ? `${(page-1)*size+1}–${Math.min(page*size,selected.length)} / ${selected.length} hồ sơ` : '0 hồ sơ';
    $('#page-number').textContent = `${page} / ${pages}`;
    $('#previous-page').disabled = page === 1; $('#next-page').disabled = page >= pages;
  }
  ['#table-search','#table-status','#table-size'].forEach(selector => $(selector)?.addEventListener('input', () => { page = 1; renderTable(); }));
  $('#previous-page')?.addEventListener('click', () => { page--; renderTable(); });
  $('#next-page')?.addEventListener('click', () => { page++; renderTable(); });
  $('#reset-filters')?.addEventListener('click', () => { $('#table-search').value = ''; $('#table-status').value = ''; page = 1; renderTable(); });
  $$('[data-sort]').forEach(button => button.addEventListener('click', () => {
    const index = Number(button.dataset.sort); sortDirection = sortIndex === index ? -sortDirection : 1; sortIndex = index;
    $$('thead th', table).forEach(th => th.setAttribute('aria-sort','none'));
    button.closest('th').setAttribute('aria-sort',sortDirection === 1 ? 'ascending' : 'descending'); renderTable();
  }));
  renderTable();
  function csvCell(value) {
    // Prevent spreadsheet formulas when exporting browser-entered/filterable text.
    const safe = /^[=+@\-\t\r]/.test(value) ? "'" + value : value;
    return '"' + safe.replace(/"/g,'""') + '"';
  }
  function exportCsv() {
    if (!table) { toast('Chưa có bảng dữ liệu để xuất.'); return; }
    const header = $$('thead th',table).slice(0,-1).map(th=>th.textContent.replace(' ↕',''));
    const data = filteredRows().map(row=>Array.from(row.cells).slice(0,-1).map(td=>td.textContent.trim()));
    const csv = '\uFEFF' + [header,...data].map(cells=>cells.map(csvCell).join(',')).join('\r\n');
    const objectUrl = URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}));
    const a=document.createElement('a');a.href=objectUrl;a.download='uccms-du-lieu-mau.csv';a.click();setTimeout(()=>URL.revokeObjectURL(objectUrl),1000);
    toast('Đã xuất dữ liệu mẫu theo bộ lọc hiện tại.');
  }
  $$('[data-action]').forEach(button => button.addEventListener('click', () => {
    const action = button.dataset.action;
    if (action === 'print') { window.print(); return; }
    if (action === 'export') { exportCsv(); return; }
    const pairs = [['Thao tác',button.getAttribute('aria-label') || button.textContent.trim()],['Phạm vi','Chỉ mô phỏng trong trình duyệt']];
    if (kind === 'permissions') $$('tbody tr').forEach(row => pairs.push([$('th',row).textContent, $$('input',row).map((x,i)=>x.checked ? ['Xem','Xử lý','Duyệt','Quản trị'][i] : '').filter(Boolean).join(', ') || 'Không có quyền']));
    review('Xác nhận thao tác mẫu',pairs,()=>toast('Đã mô phỏng thao tác. Chưa cập nhật hệ thống.'));
  }));

  // Browser draft lifecycle: passwords/files are never persisted; storage failures are visible.
  const form = $('#preview-form');
  let dirty = false, reviewing = false, fileUrl;
  const draftKey = 'uccms-ui-draft:' + (document.body.dataset.route || 'page');
  function controls() { return form ? $$('input:not([type="hidden"]), select, textarea',form) : []; }
  function fieldValue(input) { return input.type === 'checkbox' ? (input.checked ? 'Đã xác nhận' : 'Chưa xác nhận') : input.type === 'file' ? (input.files[0]?.name || 'Chưa chọn') : input.type === 'password' ? '••••••••' : input.value; }
  function labelFor(input) { return document.querySelector(`label[for="${input.id}"]`)?.textContent.replace('*','').trim() || input.name; }
  function setError(input,message) {
    input.setCustomValidity(message); input.setAttribute('aria-invalid', String(Boolean(message)));
    let error = input.closest('.field')?.querySelector('.field-error') || document.getElementById('error-' + input.id);
    if (!error && message) {
      error = document.createElement('small'); error.className = 'field-error'; error.id = 'error-' + input.id;
      error.setAttribute('aria-live','polite'); input.after(error);
      input.setAttribute('aria-describedby', ((input.getAttribute('aria-describedby') || '') + ' ' + error.id).trim());
    }
    if (error) error.textContent = message;
  }
  function showMessage(message) { const box=$('#form-message'); if(box){box.hidden=false;box.textContent=message;} }
  function saveDraft(show=true) {
    if (!form) return false;
    const data={}; controls().filter(x=>!['file','password'].includes(x.type)).forEach(x=>{data[x.name]=x.type==='checkbox'?x.checked:x.value;});
    try { sessionStorage.setItem(draftKey,JSON.stringify(data)); dirty=false; if(show)showMessage('Đã lưu nháp trong phiên trình duyệt. File và mật khẩu không được lưu; đóng phiên sẽ mất nháp.'); return true; }
    catch { showMessage('Trình duyệt không cho lưu nháp. Giữ trang này mở để tránh mất thông tin.'); return false; }
  }
  function updateCalculations() {
    const value=name=>Number(form?.elements.namedItem(name)?.value || 0);
    if ($('#contract-balance')) $('#contract-balance').textContent=money(Math.max(0,value('agreedPrice')-value('depositHeld')));
    if ($('#settlement-total')) $('#settlement-total').textContent=money(value('basisAmount')*(1-value('commissionRate')/100)-value('fixedFee')-value('approvedExpenses')-value('refundsPosted')+value('depositForfeited'));
    if(kind==='deposit'){
      const outcome=form.elements.namedItem('outcome')?.value;
      const refund=form.elements.namedItem('refundAmount'); const forfeit=form.elements.namedItem('forfeitAmount');
      if(outcome==='Hoàn toàn bộ'){refund.value='50000000';forfeit.value='0';}
      if(outcome==='Giữ toàn bộ'){refund.value='0';forfeit.value='50000000';}
      if(refund)refund.readOnly=outcome!=='Hoàn một phần';if(forfeit)forfeit.readOnly=outcome!=='Hoàn một phần';
    }
    if(kind==='contract'){
      const authorized=form.elements.namedItem('sellerPartyType')?.value==='Đại diện được ủy quyền';
      const file=form.elements.namedItem('powerOfAttorney');if(file){file.required=authorized;file.closest('.field').hidden=!authorized;}
    }
    if(kind==='inquiry' && role!=='manager' && form) { form.hidden=true; }
  }
  function validate() {
    let first = null;
    controls().forEach(input => {
      setError(input,''); let error='';
      if(input.required && (input.type==='checkbox' ? !input.checked : input.type==='file' ? !input.files.length : !input.value.trim())) error='Vui lòng nhập / xác nhận '+labelFor(input).toLocaleLowerCase('vi')+'.';
      else if(input.type==='email' && input.value && !input.validity.valid) error='Email chưa đúng định dạng.';
      else if(input.type==='number' && input.value && (!Number.isFinite(Number(input.value)) || Number(input.value)<0)) error='Nhập số hợp lệ, không âm.';
      else if(input.name==='commissionRate' && Number(input.value)>100) error='Hoa hồng phải từ 0 đến 100%.';
      else if(input.name==='phone' && input.value && !/^\d{10,11}$/.test(input.value)) error='Số điện thoại cần 10–11 chữ số.';
      else if(input.name==='idNumber' && input.value && !/^\d{12}$/.test(input.value)) error='CCCD cần 12 chữ số.';
      else if(input.name==='vin' && input.value && !/^[A-HJ-NPR-Z0-9]{17}$/i.test(input.value)) error='VIN cần 17 ký tự và không gồm I, O, Q.';
      else if(input.name==='otp' && input.value && !/^\d{6}$/.test(input.value)) error='OTP cần đúng 6 chữ số.';
      else if(['keysCount','mileage','odometer'].includes(input.name) && input.value && !Number.isInteger(Number(input.value))) error='Nhập số nguyên không âm.';
      else if(input.name==='manufactureYear' && input.value && (!Number.isInteger(Number(input.value)) || Number(input.value)<1900 || Number(input.value)>2026)) error='Năm sản xuất cần từ 1900 đến 2026.';
      else if(input.type==='file' && input.files.length) {
        const f=input.files[0];if(f.size>10*1024*1024)error='File vượt quá 10 MB.';
        else if(!/\.(pdf|jpe?g|png)$/i.test(f.name))error='Chọn PDF, JPG hoặc PNG.';
      }
      setError(input,error);if(error&&!first)first=input;
    });
    const val=n=>Number(form.elements.namedItem(n)?.value||0);
    function businessError(name,message){const x=form.elements.namedItem(name);if(x){setError(x,message);if(!first)first=x;}}
    if(kind==='deposit'&&Math.round(val('refundAmount')+val('forfeitAmount'))!==50000000)businessError('refundAmount','Tiền hoàn + tiền giữ phải bằng 50.000.000 ₫ cọc đã thu.');
    if(kind==='contract'&&val('depositHeld')>val('agreedPrice'))businessError('depositHeld','Cọc không được vượt giá thỏa thuận.');
    if(form.elements.namedItem('validFrom')&&form.elements.namedItem('validTo')?.value<form.elements.namedItem('validFrom').value)businessError('validTo','Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.');
    if(form.elements.namedItem('from')&&form.elements.namedItem('to')?.value<form.elements.namedItem('from').value)businessError('to','Khoảng ngày báo cáo chưa hợp lệ.');
    if(kind==='refund' && val('amount')>50000000)businessError('amount','Số tiền hoàn không được vượt khoản thu mẫu 50.000.000 ₫.');
    if(kind==='settlement'&&Number($('#settlement-total').textContent.replace(/[^\d-]/g,''))<0)businessError('approvedExpenses','Số tiền chi trả không thể âm.');
    if(first){showMessage('Có thông tin chưa hợp lệ. Vui lòng kiểm tra các trường được đánh dấu.');first.focus();return false;}
    const blockers=$$('[data-passed="false"]');
    const action=form.elements.namedItem('decision')?.value;
    if(blockers.length&&(['handover','listing','contract','reservation','refund','payout'].includes(kind)||kind==='decision'&&action!=='Yêu cầu bổ sung'&&action!=='Từ chối')) {
      showMessage('Chưa thể mô phỏng hoàn tất: '+blockers.map(x=>$('strong',x).textContent).join('; ')+'.');$('#form-message').scrollIntoView({block:'center'});return false;
    }
    return true;
  }
  if(form){
    ['#save-draft','#clear-draft','#review-submit'].forEach(selector=>{const button=$(selector);if(button)button.disabled=false;});
    // Report filters have fixed fixture numbers; clearly show the selected interval only.
    if(kind==='report') { $('#save-draft').hidden=true;$('#clear-draft').hidden=true; }
    try{const saved=JSON.parse(sessionStorage.getItem(draftKey)||'null');if(saved){controls().filter(x=>!['file','password'].includes(x.type)).forEach(x=>{if(Object.hasOwn(saved,x.name)){if(x.type==='checkbox')x.checked=Boolean(saved[x.name]);else x.value=saved[x.name];}});showMessage('Đã khôi phục nháp trong phiên trình duyệt. Chọn lại file nếu có.');}}
    catch{showMessage('Không thể khôi phục nháp. Có thể tiếp tục nhập thông tin.');}
    form.addEventListener('input',e=>{dirty=true;if(e.target.matches('input,select,textarea'))setError(e.target,'');updateCalculations();});
    form.addEventListener('change',updateCalculations);updateCalculations();
    $('#save-draft')?.addEventListener('click',()=>saveDraft());
    $('#clear-draft')?.addEventListener('click',()=>review('Xóa nháp trên trình duyệt',[['Phạm vi','Nháp của màn hình đang mở']],()=>{try{sessionStorage.removeItem(draftKey);form.reset();dirty=false;updateCalculations();controls().forEach(x=>setError(x,''));showMessage('Đã xóa nháp của màn hình này.');}catch{showMessage('Không thể xóa nháp vì trình duyệt chặn lưu trữ.');}}));
    form.addEventListener('submit',e=>{
      e.preventDefault();if(reviewing||!validate())return;
      const pairs=controls().filter(x=>!x.closest('[hidden]')).map(x=>[labelFor(x),fieldValue(x)]);
      reviewing=true;const submit=$('#review-submit');submit.disabled=true;$('[data-submit-label]',submit).textContent='Đang chuẩn bị bản xem trước…';
      review('Kiểm tra trước khi mô phỏng',pairs,()=>{
        saveDraft(false);const box=$('#preview-result');box.hidden=false;box.replaceChildren();
        const title=document.createElement('h3');title.textContent=kind==='report'?'Khoảng ngày đã chọn':'Đã mô phỏng gửi thông tin';
        const message=document.createElement('p');message.textContent=kind==='report'?'Chỉ số vẫn là dữ liệu mẫu cố định; chưa truy vấn báo cáo thật.':'Nháp được giữ trong phiên trình duyệt. Chưa gửi dữ liệu, upload file hoặc thực hiện giao dịch trên máy chủ.';
        box.append(title,message);
        const details=document.createElement('details');const summary=document.createElement('summary');summary.textContent='Xem thông tin vừa nhập';details.append(summary);
        pairs.forEach(([label,value])=>{const p=document.createElement('p');p.textContent=label+': '+value;details.append(p);});box.append(details);
        toast('Đã tạo bản xem trước. Không có thay đổi trên backend.');box.scrollIntoView({block:'center'});
      });
    });
    dialog?.addEventListener('close',()=>{reviewing=false;const button=$('#review-submit');button.disabled=false;$('[data-submit-label]',button).textContent=kind==='report'?'Xem khoảng ngày mẫu':'Kiểm tra & mô phỏng gửi';});
    controls().filter(x=>x.type==='file').forEach(input=>input.addEventListener('change',()=>{
      const file=input.files[0];const preview=$('#upload-preview');if(!preview)return;
      if(fileUrl)URL.revokeObjectURL(fileUrl);preview.hidden=true;
      if(!file||file.size>10*1024*1024||!/\.(pdf|jpe?g|png)$/i.test(file.name))return;
      fileUrl=URL.createObjectURL(file);$('#upload-name').textContent=file.name+' · '+(file.size/1024).toFixed(1)+' KB';
      const image=$('#upload-image');const a=$('#upload-file-link');image.hidden=!file.type.startsWith('image/');a.hidden=false;a.href=fileUrl;if(!image.hidden)image.src=fileUrl;preview.hidden=false;
    }));
    window.addEventListener('beforeunload',e=>{if(dirty){e.preventDefault();e.returnValue='';}});
    window.addEventListener('pagehide',()=>{if(fileUrl)URL.revokeObjectURL(fileUrl);});
  }
  const routeSearch=$('#route-search');
  routeSearch?.addEventListener('input',()=>{
    const text=normalize(routeSearch.value);let count=0;
    $$('.role-card').forEach(card=>{
      const roleMatch=normalize($('h2',card).textContent).includes(text);
      let visible=0;$$('.url-list li',card).forEach(li=>{li.hidden=Boolean(text)&&!roleMatch&&!normalize(li.textContent).includes(text);if(!li.hidden)visible++;});
      card.hidden=visible===0;count+=visible;$('details',card).open=Boolean(text)&&visible>0;
    });$('#routes-empty').hidden=count>0;
  });
})();
