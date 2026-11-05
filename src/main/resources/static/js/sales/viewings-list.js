(() => {
  const form = document.getElementById('viewings-filter-form');
  if (!form) return;

  const submitFilters = () => form.requestSubmit();
  form.querySelectorAll('input[type="date"], select').forEach((field) => {
    field.addEventListener('change', submitFilters);
  });
  document.getElementById('viewing-size')?.addEventListener('change', submitFilters);
})();
