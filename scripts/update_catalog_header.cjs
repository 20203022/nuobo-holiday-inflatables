const fs = require('node:fs');
const path = require('node:path');
const file = path.resolve(__dirname, '../product-list.html');
const source = fs.readFileSync(file, 'utf8');
const before = '<section class="product-catalog-hero">';
const after = '<section class="product-catalog-hero" th:classappend="${currentPage == \'halloween\'} ? \' is-halloween\' : \'\'">';
if (!source.includes(before)) throw new Error('Catalog header was not found');
fs.writeFileSync(file, source.replace(before, after));
