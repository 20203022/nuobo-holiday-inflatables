const fs = require('node:fs');
const path = require('node:path');
for (const name of ['product-list.html', 'product-detail.html']) {
  const file = path.resolve(__dirname, '..', name);
  const source = fs.readFileSync(file, 'utf8');
  const start = source.indexOf('<aside class="bulk-inquiry-bar"');
  const end = source.indexOf('</aside>', start);
  if (start < 0 || end < 0) throw new Error('Inquiry bar missing in ' + name);
  fs.writeFileSync(file, source.slice(0, start) + source.slice(end + '</aside>'.length));
}
