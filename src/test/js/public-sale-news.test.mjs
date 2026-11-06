import { readFile } from 'node:fs/promises';
import test from 'node:test';
import assert from 'node:assert/strict';
const source = await readFile(new URL('../../main/resources/static/js/app/public-sale-news.js', import.meta.url), 'utf8');
const { filterListings, paginate, validPhone, listings } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);

test('search ignores Vietnamese accents and combines with brand and budget', () => {
  const result = filterListings(listings, { q: 'ha noi 2021', brand: 'Toyota', budget: 'under-500' });
  assert.deepEqual(result.map(car => car.id), [522]);
  assert.equal(filterListings(listings, { q: 'khong-co-xe-nay' }).length, 0);
  assert.equal(filterListings(listings, { q: 'dien' })[0].brand, 'VinFast');
});
test('budget boundaries do not overlap and price sorts are numeric', () => {
  const cars = [499999999, 500000000, 799999999, 800000000, 999999999, 1000000000].map((price, id) => ({ id, price, title: '', published: '2026-11-01' }));
  assert.deepEqual(filterListings(cars, { budget: 'under-500' }).map(car => car.price), [499999999]);
  assert.deepEqual(filterListings(cars, { budget: '500-800', sort: 'price-asc' }).map(car => car.price), [500000000, 799999999]);
  assert.deepEqual(filterListings(cars, { budget: '800-1000', sort: 'price-desc' }).map(car => car.price), [999999999, 800000000]);
  assert.deepEqual(filterListings(cars, { budget: 'over-1000' }).map(car => car.price), [1000000000]);
  assert.equal(filterListings(listings, { sort: 'km-asc' })[0].id, 520);
});
test('pagination clamps invalid or stale pages after filtering', () => {
  assert.equal(paginate(listings, 1).items.length, 6);
  assert.equal(paginate(listings, 2).items[0].id, 522);
  assert.equal(paginate(listings, 999).page, 2);
  assert.equal(paginate(listings, -2).page, 1);
  const empty = paginate([], 8);
  assert.equal(empty.page, 1); assert.equal(empty.total, 0); assert.deepEqual(empty.items, []);
});
test('contact phone validation accepts local and international mobile formats', () => {
  for (const phone of ['0912 345 678', '+84 912 345 678', '032-123-4567']) assert.equal(validPhone(phone), true);
  for (const phone of ['', '123', 'aaaaaaaaaa', '0112345678', '091234567890', '+1 9123456789']) assert.equal(validPhone(phone), false);
});
