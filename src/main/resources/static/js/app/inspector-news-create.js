/* Nguyễn Vũ Trường An: màn Đăng tin: định dạng giá, chọn ảnh, xem trước và kiểm tra trước khi đăng. */
(() => {
  'use strict';
  const MAX_PHOTOS = 10;
  const MAX_BYTES = 10 * 1024 * 1024;
  const ALLOWED = /\.(jpe?g|png)$/i;

  const root = document.getElementById('news-create');
  const form = root?.querySelector('#news-form');
  if (!form) return;
  const $ = (selector) => root.querySelector(selector);

  const title = $('#news-title');
  const body = $('#news-body');
  const price = $('#news-price');
  const listPrice = form.elements.listPrice;
  const photoInput = $('[data-photo-input]');
  const photoGrid = $('[data-photo-grid]');
  const photoAdd = $('[data-photo-add]');
  const photoCount = $('[data-photo-count]');
  const photoError = $('[data-photo-error]');
  const cover = $('[data-preview-cover]');
  const coverIcon = cover.querySelector('svg');
  const previewTitle = $('[data-preview-title]');
  const previewPrice = $('[data-preview-price]');
  const publish = $('[data-publish]');
  const flash = $('#news-flash');

  let photos = [];

  function formatPrice() {
    const digits = price.value.replace(/\D/g, '').replace(/^0+/, '');
    listPrice.value = digits;
    price.value = digits ? Number(digits).toLocaleString('vi-VN') : '';
  }

  function setPreviewText(element, value, emptyText) {
    element.textContent = value || emptyText;
    element.classList.toggle('is-empty', !value);
  }

  function setCheck(key, ok) {
    const item = root.querySelector(`[data-check="${key}"]`);
    item.classList.toggle('is-done', ok);
    item.classList.toggle('is-pending', !ok);
    return ok;
  }

  function refresh() {
    root.querySelector('[data-count="title"]').textContent = `${title.value.length}/200`;
    root.querySelector('[data-count="body"]').textContent = `${body.value.length}/8000`;
    setPreviewText(previewTitle, title.value.trim(), 'Tiêu đề tin');
    setPreviewText(previewPrice, price.value ? `${price.value} ₫` : '', 'Giá niêm yết');

    const checks = [
      setCheck('vehicle', Boolean(form.elements.vehicleId)),
      setCheck('title', title.value.trim() !== ''),
      setCheck('body', body.value.trim() !== ''),
      setCheck('price', listPrice.value !== ''),
      setCheck('photos', photos.length > 0),
    ];
    publish.disabled = checks.includes(false);
  }

  function renderPhotos() {
    photoGrid.querySelectorAll('.photo-slot').forEach((slot) => slot.remove());
    photos.forEach((photo, index) => {
      const slot = document.createElement('div');
      slot.className = 'photo-slot';
      const image = document.createElement('img');
      image.src = photo.url;
      image.alt = photo.file.name;
      slot.append(image);
      if (index === 0) {
        const label = document.createElement('span');
        label.className = 'photo-label';
        label.textContent = 'Ảnh bìa';
        slot.append(label);
      }
      const remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'photo-remove';
      remove.textContent = '×';
      remove.setAttribute('aria-label', `Bỏ ảnh ${photo.file.name}`);
      remove.addEventListener('click', () => {
        URL.revokeObjectURL(photo.url);
        photos = photos.filter((x) => x !== photo);
        photoError.hidden = true;
        renderPhotos();
      });
      slot.append(remove);
      photoGrid.insertBefore(slot, photoAdd);
    });

    // input.files chỉ đổi được qua DataTransfer; form gửi đúng các ảnh đang hiện.
    const transfer = new DataTransfer();
    photos.forEach((photo) => transfer.items.add(photo.file));
    photoInput.files = transfer.files;

    photoAdd.hidden = photos.length >= MAX_PHOTOS;
    photoCount.textContent = `${photos.length}/${MAX_PHOTOS} ảnh`;
    cover.querySelector('img')?.remove();
    coverIcon.style.display = photos.length ? 'none' : '';
    if (photos.length) {
      const image = document.createElement('img');
      image.src = photos[0].url;
      image.alt = '';
      cover.append(image);
    }
    refresh();
  }

  photoInput.addEventListener('change', () => {
    const errors = [];
    Array.from(photoInput.files).forEach((file) => {
      if (photos.length >= MAX_PHOTOS) errors.push(`Chỉ được tối đa ${MAX_PHOTOS} ảnh.`);
      else if (!ALLOWED.test(file.name)) errors.push(`${file.name}: chỉ nhận JPG hoặc PNG.`);
      else if (file.size > MAX_BYTES) errors.push(`${file.name}: ảnh vượt quá 10MB.`);
      else photos.push({ file, url: URL.createObjectURL(file) });
    });
    photoError.textContent = [...new Set(errors)].join(' ');
    photoError.hidden = errors.length === 0;
    renderPhotos();
  });

  title.addEventListener('input', refresh);
  body.addEventListener('input', refresh);
  price.addEventListener('input', () => {
    formatPrice();
    refresh();
  });

  // Chờ BE: bỏ khối này để form POST thật tới /inspector/listings.
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    const status = form.elements.status?.value === 'PAUSED' ? 'Tạm ẩn' : 'Đang hiển thị';
    flash.textContent = `UI demo: đã lưu tin "${title.value.trim()}" · ${status} (chưa lưu BE).`;
    flash.hidden = false;
    flash.scrollIntoView({ block: 'center' });
  });

  refresh();
})();
