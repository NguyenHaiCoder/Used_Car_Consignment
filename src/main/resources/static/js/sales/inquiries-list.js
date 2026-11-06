(() => {
  const form = document.getElementById('inquiry-list-filters');
  if (!form) return;

  ['salespersonId', 'status', 'size'].forEach(id => {
    document.getElementById(id).addEventListener('change', () => form.requestSubmit());
  });
})();
