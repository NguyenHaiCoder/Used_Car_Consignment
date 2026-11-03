(() => {
  const form = document.getElementById('buyer-list-filters');
  if (!form) return;

  const search = document.getElementById('buyer-search');
  const filters = ['buyer-status', 'buyer-created-sort', 'buyer-size'];
  const idSort = document.getElementById('buyer-id-sort');
  const createdSort = document.getElementById('buyer-created-sort');
  let searchTimer;

  const apply = () => {
    clearTimeout(searchTimer);
    form.requestSubmit();
  };

  form.addEventListener('submit', () => clearTimeout(searchTimer));
  search.addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(apply, 300);
  });
  filters.forEach(id => document.getElementById(id).addEventListener('change', apply));
  document.getElementById('buyer-id-sort-toggle').addEventListener('click', () => {
    idSort.value = idSort.value === 'asc' ? 'desc' : 'asc';
    createdSort.value = 'default';
    apply();
  });
})();
