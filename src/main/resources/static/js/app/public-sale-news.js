/* UI demo only. Listings and contact confirmations stay in this browser page. */
const photo = (id) => `https://images.unsplash.com/${id}?auto=format&fit=crop&w=960&q=80`;
export const listings = [
  { id: 528, brand: 'Hyundai', title: 'Hyundai Tucson 2.0 2022', year: 2022, km: 32000, fuel: 'Xăng', price: 769000000, location: 'Hà Nội', published: '2026-11-06', image: photo('photo-1503736334956-4c8f8e92946d') },
  { id: 527, brand: 'Toyota', title: 'Toyota Camry 2.0G 2020', year: 2020, km: 62100, fuel: 'Xăng', price: 920000000, location: 'TP. Hồ Chí Minh', published: '2026-11-05', image: photo('photo-1502877338535-766e1452684a') },
  { id: 526, brand: 'Mazda', title: 'Mazda CX-5 2.0 Luxury 2021', year: 2021, km: 48500, fuel: 'Xăng', price: 760000000, location: 'Hà Nội', published: '2026-11-04', image: photo('photo-1503376780353-7e6692767b70') },
  { id: 525, brand: 'Honda', title: 'Honda CR-V 1.5L 2022', year: 2022, km: 27400, fuel: 'Xăng', price: 1080000000, location: 'Đà Nẵng', published: '2026-11-03', image: photo('photo-1503736334956-4c8f8e92946d') },
  { id: 524, brand: 'Kia', title: 'Kia Seltos 1.6 Premium 2023', year: 2023, km: 18200, fuel: 'Xăng', price: 680000000, location: 'TP. Hồ Chí Minh', published: '2026-11-02', image: photo('photo-1503736334956-4c8f8e92946d') },
  { id: 523, brand: 'Ford', title: 'Ford Ranger Wildtrak 2020', year: 2020, km: 76000, fuel: 'Dầu', price: 845000000, location: 'Hà Nội', published: '2026-11-01', image: photo('photo-1616788494707-ec28f08d05a1') },
  { id: 522, brand: 'Toyota', title: 'Toyota Vios 1.5G 2021', year: 2021, km: 54200, fuel: 'Xăng', price: 425000000, location: 'Hà Nội', published: '2026-10-31', image: photo('photo-1502877338535-766e1452684a') },
  { id: 521, brand: 'Mitsubishi', title: 'Mitsubishi Xpander 1.5 AT 2023', year: 2023, km: 21300, fuel: 'Xăng', price: 610000000, location: 'Đà Nẵng', published: '2026-10-30', image: photo('photo-1503736334956-4c8f8e92946d') },
  { id: 520, brand: 'VinFast', title: 'VinFast VF 8 Plus 2023', year: 2023, km: 15600, fuel: 'Điện', price: 890000000, location: 'TP. Hồ Chí Minh', published: '2026-10-29', image: photo('photo-1593941707874-ef25b8b4a92b') },
  { id: 519, brand: 'Honda', title: 'Honda City RS 2022', year: 2022, km: 31000, fuel: 'Xăng', price: 515000000, location: 'Hà Nội', published: '2026-10-28', image: photo('photo-1503736334956-4c8f8e92946d') },
  { id: 518, brand: 'Hyundai', title: 'Hyundai Accent 1.4 AT 2022', year: 2022, km: 33100, fuel: 'Xăng', price: 465000000, location: 'Đà Nẵng', published: '2026-10-27', image: photo('photo-1502877338535-766e1452684a') },
  { id: 517, brand: 'Toyota', title: 'Toyota Fortuner 2.4 AT 2021', year: 2021, km: 58400, fuel: 'Dầu', price: 1020000000, location: 'TP. Hồ Chí Minh', published: '2026-10-26', image: photo('photo-1616788494707-ec28f08d05a1') },
];
const budgets = { 'under-500': [0, 500000000], '500-800': [500000000, 800000000], '800-1000': [800000000, 1000000000], 'over-1000': [1000000000, Infinity] };
const sorts = ['newest', 'price-asc', 'price-desc', 'km-asc'];
export const normalize = (text) => String(text).normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[đĐ]/g, 'd').toLowerCase();
export const formatPrice = (value) => `${new Intl.NumberFormat('vi-VN').format(value)} ₫`;
export const validPhone = (value) => /^(?:0[35789]\d{8}|\+84[35789]\d{8})$/.test(value.replace(/[\s.()-]/g, ''));

export function filterListings(items, { q = '', brand = '', budget = '', sort = 'newest' } = {}) {
  const terms = normalize(q.trim()).split(/\s+/).filter(Boolean);
  const [min, max] = budgets[budget] || [0, Infinity];
  const result = items.filter((car) => {
    const text = normalize(`${car.title} ${car.brand} ${car.year} ${car.fuel} ${car.location}`);
    return (!brand || car.brand === brand) && car.price >= min && car.price < max && terms.every((term) => text.includes(term));
  });
  return result.sort((a, b) => {
    if (sort === 'price-asc') return a.price - b.price || b.id - a.id;
    if (sort === 'price-desc') return b.price - a.price || b.id - a.id;
    if (sort === 'km-asc') return a.km - b.km || b.id - a.id;
    return b.published.localeCompare(a.published) || b.id - a.id;
  });
}

export function paginate(items, requestedPage, pageSize = 6) {
  const pages = Math.max(1, Math.ceil(items.length / pageSize));
  const page = Math.min(pages, Math.max(1, Math.floor(Number(requestedPage)) || 1));
  const start = (page - 1) * pageSize;
  return { page, pages, total: items.length, start, items: items.slice(start, start + pageSize) };
}

function initialize() {
  const root = document.getElementById('sale-news-page');
  if (!root) return;
  const form = document.getElementById('listing-filters');
  const search = document.getElementById('listing-search');
  const brand = document.getElementById('listing-brand');
  const budget = document.getElementById('listing-budget');
  const sort = document.getElementById('listing-sort');
  const grid = document.getElementById('listing-grid');
  const count = document.getElementById('listing-count');
  const empty = document.getElementById('no-listings');
  const clear = document.getElementById('clear-filters');
  const pagination = document.getElementById('listing-pagination');
  const template = document.getElementById('listing-card-template');
  const dialog = document.getElementById('contact-dialog');
  const contactForm = document.getElementById('listing-contact-form');
  const success = document.getElementById('contact-success');
  const phone = document.getElementById('contact-phone');
  const name = document.getElementById('contact-name');
  const brands = [...new Set(listings.map((car) => car.brand))].sort();
  brands.forEach((value) => brand.add(new Option(value, value)));
  let state;
  let searchTimer;
  let selectedCar;
  let contactOpener;

  function readUrl() {
    const params = new URLSearchParams(location.search);
    state = {
      q: (params.get('q') || '').slice(0, 100),
      brand: brands.includes(params.get('brand')) ? params.get('brand') : '',
      budget: Object.hasOwn(budgets, params.get('budget')) ? params.get('budget') : '',
      sort: sorts.includes(params.get('sort')) ? params.get('sort') : 'newest',
      page: /^\d+$/.test(params.get('page') || '') ? Number(params.get('page')) : 1,
    };
    search.value = state.q; brand.value = state.brand; budget.value = state.budget; sort.value = state.sort;
  }

  function writeUrl(push = false) {
    const url = new URL(location.href);
    ['q', 'brand', 'budget', 'sort', 'page'].forEach((key) => {
      if (state[key] && !(key === 'page' && state.page === 1) && !(key === 'sort' && state.sort === 'newest')) url.searchParams.set(key, state[key]);
      else url.searchParams.delete(key);
    });
    if (url.href !== location.href) history[push ? 'pushState' : 'replaceState'](null, '', url);
  }

  const specs = (car) => `${car.year} · ${new Intl.NumberFormat('vi-VN').format(car.km)} km · ${car.fuel}`;

  function openContact(car, opener) {
    selectedCar = car; contactOpener = opener;
    contactForm.reset(); phone.setCustomValidity(''); name.setCustomValidity('');
    contactForm.hidden = false; success.hidden = true;
    document.getElementById('contact-car-title').textContent = car.title;
    document.getElementById('contact-car-specs').textContent = specs(car);
    document.getElementById('contact-car-price').textContent = formatPrice(car.price);
    document.getElementById('contact-listing').value = car.id;
    dialog.showModal(); name.focus();
  }

  function pageButton(label, target, disabled = false, current = false) {
    const button = document.createElement('button');
    button.type = 'button'; button.className = 'page-button'; button.textContent = label; button.disabled = disabled;
    button.setAttribute('aria-label', /^\d+$/.test(label) ? `Trang ${label}` : label);
    if (current) button.setAttribute('aria-current', 'page');
    button.addEventListener('click', () => {
      state.page = target; render(); writeUrl(true);
      root.scrollIntoView({ block: 'start', behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' });
      count.setAttribute('tabindex', '-1'); count.focus({ preventScroll: true });
    });
    return button;
  }

  function render() {
    const result = paginate(filterListings(listings, state), state.page);
    state.page = result.page;
    count.textContent = result.total ? `Tìm thấy ${result.total} xe · Hiển thị ${result.start + 1}–${result.start + result.items.length}` : 'Không tìm thấy xe phù hợp';
    clear.hidden = !state.q && !state.brand && !state.budget && state.sort === 'newest';
    empty.hidden = result.total > 0;
    grid.replaceChildren();
    result.items.forEach((car) => {
      const fragment = template.content.cloneNode(true);
      const card = fragment.querySelector('article');
      const image = card.querySelector('img');
      image.alt = `Ảnh minh họa ${car.title}`;
      image.addEventListener('error', () => { image.hidden = true; card.querySelector('.photo-fallback').hidden = false; }, { once: true });
      image.src = car.image;
      card.querySelector('.listing-reference').textContent = `TD-${String(car.id).padStart(6, '0')}`;
      card.querySelector('.listing-title').textContent = car.title;
      card.querySelector('.listing-price').textContent = formatPrice(car.price);
      card.querySelector('.listing-specs').textContent = specs(car);
      card.querySelector('.listing-location').textContent = car.location;
      const contact = card.querySelector('.listing-contact');
      contact.setAttribute('aria-label', `Liên hệ về ${car.title}`);
      contact.addEventListener('click', () => openContact(car, contact));
      grid.append(fragment);
    });
    pagination.replaceChildren(); pagination.hidden = result.pages === 1;
    if (result.pages > 1) {
      pagination.append(pageButton('Trang trước', result.page - 1, result.page === 1));
      for (let page = 1; page <= result.pages; page++) pagination.append(pageButton(String(page), page, false, page === result.page));
      pagination.append(pageButton('Trang sau', result.page + 1, result.page === result.pages));
    }
  }

  function update(push) {
    state = { q: search.value.trim(), brand: brand.value, budget: budget.value, sort: sort.value, page: 1 };
    render(); writeUrl(push);
  }
  function reset() {
    clearTimeout(searchTimer); form.reset(); sort.value = 'newest'; update(true); search.focus();
  }
  form.addEventListener('submit', (event) => { event.preventDefault(); clearTimeout(searchTimer); update(true); });
  search.addEventListener('input', () => { clearTimeout(searchTimer); searchTimer = setTimeout(() => update(false), 180); });
  [brand, budget, sort].forEach((input) => input.addEventListener('change', () => { clearTimeout(searchTimer); update(true); }));
  clear.addEventListener('click', reset);
  document.getElementById('empty-reset').addEventListener('click', reset);
  window.addEventListener('popstate', () => { clearTimeout(searchTimer); readUrl(); render(); });

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
    if (!selectedCar || !contactForm.reportValidity()) return;
    const need = contactForm.querySelector('[name="need"]:checked').nextElementSibling.textContent.toLowerCase();
    document.getElementById('contact-success-message').textContent = `${name.value.trim()}, bạn đã gửi yêu cầu ${need} ${selectedCar.title}. Số liên hệ: ${phone.value.trim()}.`;
    contactForm.hidden = true; success.hidden = false;
    success.querySelector('button').focus();
  });

  readUrl(); render(); writeUrl();
}
if (typeof document !== 'undefined') initialize();
