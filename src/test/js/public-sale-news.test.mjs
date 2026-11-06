import { readFile } from 'node:fs/promises';
import test from 'node:test';
import assert from 'node:assert/strict';
const source = await readFile(new URL('../../main/resources/static/js/app/public-sale-news.js', import.meta.url), 'utf8');
const { validPhone } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);

test('contact phone validation accepts local and international mobile formats', () => {
  for (const phone of ['0912 345 678', '+84 912 345 678', '032-123-4567']) assert.equal(validPhone(phone), true);
  for (const phone of ['', '123', 'aaaaaaaaaa', '0112345678', '091234567890', '+1 9123456789']) assert.equal(validPhone(phone), false);
});
