/* Tin bán xe: danh sách, bộ lọc và phân trang do server render (Thymeleaf).
   File này chỉ mở hộp thoại liên hệ; form liên hệ vẫn là demo, chưa gửi về server. */
export const validPhone = (value) => /^(?:0[35789]\d{8}|\+84[35789]\d{8})$/.test(value.replace(/[\s.()-]/g, ''));

function initialize() {
  const root = document.getElementById('sale-news-page');
  const dialog = document.getElementById('contact-dialog');
  if (!root || !dialog) return;
  const contactForm = document.getElementById('listing-contact-form');
  const success = document.getElementById('contact-success');
  const phone = document.getElementById('contact-phone');
  const name = document.getElementById('contact-name');
  let selectedTitle = '';
  let contactOpener;

  function openContact(button) {
    contactOpener = button;
    selectedTitle = button.dataset.title || '';
    contactForm.reset(); phone.setCustomValidity(''); name.setCustomValidity('');
    contactForm.hidden = false; success.hidden = true;
    document.getElementById('contact-car-title').textContent = selectedTitle;
    document.getElementById('contact-car-specs').textContent = button.dataset.specs || '';
    document.getElementById('contact-car-price').textContent = button.dataset.price || '';
    document.getElementById('contact-listing').value = button.dataset.listingId || '';
    dialog.showModal(); name.focus();
  }

  root.querySelectorAll('.listing-contact').forEach((button) => button.addEventListener('click', () => openContact(button)));

  dialog.querySelectorAll('[data-close-contact]').forEach((button) => button.addEventListener('click', () => dialog.close()));
  dialog.addEventListener('click', (event) => {
    if (event.target !== dialog) return;
    const rect = dialog.getBoundingClientRect();
    if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) dialog.close();
  });
  dialog.addEventListener('close', () => { if (contactOpener?.isConnected) contactOpener.focus(); });
  name.addEventListener('input', () => name.setCustomValidity(name.value && name.value.trim().length < 2 ? 'Vui lòng nhập họ và tên, ít nhất 2 ký tự.' : ''));
  phone.addEventListener('input', () => phone.setCustomValidity(phone.value && !validPhone(phone.value) ? 'Vui lòng nhập số di động Việt Nam hợp lệ (10 số hoặc +84).' : ''));
  contactForm.addEventListener('submit', (event) => {
    event.preventDefault();
    if (!contactForm.reportValidity()) return;
    const need = contactForm.querySelector('[name="need"]:checked').nextElementSibling.textContent.toLowerCase();
    document.getElementById('contact-success-message').textContent = `${name.value.trim()}, bạn đã gửi yêu cầu ${need} ${selectedTitle}. Số liên hệ: ${phone.value.trim()}.`;
    contactForm.hidden = true; success.hidden = false;
    success.querySelector('button').focus();
  });
}
if (typeof document !== 'undefined') initialize();
