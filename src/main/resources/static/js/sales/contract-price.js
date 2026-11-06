(() => {
  const price = document.querySelector('[data-sale-price]');
  if (!price || !price.form) return;

  const digits = value => value.replace(/\D/g, '');
  const format = value => digits(value).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  const caretAfter = (value, digitCount, skipSeparator = true) => {
    if (digitCount === 0) return 0;
    let seen = 0;
    for (let index = 0; index < value.length; index++) {
      if (/\d/.test(value[index])) seen++;
      if (seen === digitCount) {
        const next = index + 1;
        return skipSeparator && value[next] === '.' ? next + 1 : next;
      }
    }
    return value.length;
  };

  // A BigDecimal may render as 3000000.00 or 3.000.000,00 depending on locale.
  const decimal = /^(\d[\d.,]*)[.,](\d{1,2})$/.exec(price.value);
  const initial = decimal
    ? (BigInt(digits(decimal[1])) + (Number(decimal[2].padEnd(2, '0')) >= 50 ? 1n : 0n)).toString()
    : price.value;
  price.value = format(initial);
  let previousValue = price.value;

  const update = event => {
    if (event.isComposing) return;
    const raw = price.value;
    const cursor = price.selectionStart ?? raw.length;
    const count = digits(raw.slice(0, cursor)).length;
    const formatted = format(raw);
    const deletedOnlySeparator = event.inputType?.startsWith('delete')
      && digits(raw) === digits(previousValue) && raw.length < previousValue.length;
    price.value = formatted;
    const next = caretAfter(formatted, count,
      !(deletedOnlySeparator && event.inputType === 'deleteContentBackward'));
    price.setSelectionRange(next, next);
    previousValue = formatted;
  };

  price.addEventListener('input', update);
  price.addEventListener('compositionend', update);
  price.form.addEventListener('submit', () => {
    price.value = digits(price.value);
  });
})();
