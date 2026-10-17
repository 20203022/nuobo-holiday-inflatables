const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
const source = path.join(root, 'output/globe-build');
const context = vm.createContext({});
for (const name of ['d3-array.js', 'd3-geo.js']) {
  vm.runInContext(fs.readFileSync(path.join(source, name), 'utf8'), context);
}
const land = JSON.parse(fs.readFileSync(path.join(source, 'land.geojson'), 'utf8'));
const points = [];
const samples = 24000;
const goldenAngle = Math.PI * (3 - Math.sqrt(5));
for (let index = 0; index < samples; index++) {
  const y = 1 - 2 * (index + 0.5) / samples;
  const lat = Math.asin(y) * 180 / Math.PI;
  const lon = ((index * goldenAngle * 180 / Math.PI) % 360) - 180;
  if (context.d3.geoContains(land, [lon, lat])) {
    points.push([Number(lat.toFixed(3)), Number(lon.toFixed(3))]);
  }
}
fs.writeFileSync(path.join(root, 'images/globe/land-points.json'), JSON.stringify(points));
const controls = path.join(root, 'vendor/OrbitControls.js');
const code = fs.readFileSync(controls, 'utf8');
fs.writeFileSync(controls, code.replace("from 'three'", "from './three.module.js'"));
// Reuse Natural Earth's outline for the non-WebGL fallback.
const projection = context.d3.geoEquirectangular().fitExtent([[20, 20], [980, 460]], land);
const outline = context.d3.geoPath(projection)(land);
const marker = projection([120.075, 29.306]);
const svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 480"><path fill="#397b85" stroke="#7dc7ca" stroke-width=".5" d="' + outline + '"/><circle fill="#efbd71" cx="' + marker[0] + '" cy="' + marker[1] + '" r="7"/><text fill="#ffffff" font-family="Arial,sans-serif" font-size="20" x="' + (marker[0] + 12) + '" y="' + marker[1] + '">Yiwu</text></svg>';
fs.writeFileSync(path.join(root, 'images/globe/world-markets.svg'), svg);
console.log('Generated ' + points.length + ' land points and world-map fallback.');
