const fs = require('fs');
const html = fs.readFileSync('gemini_dump.html', 'utf8');
const pos = 775460;
const scriptTags = [...html.matchAll(/<script/gi)].map(m => m.index);
let scriptStart = 0;
for (const idx of scriptTags) {
  if (idx <= pos) scriptStart = idx;
  else break;
}
console.log('Script starts at:', scriptStart, 'distance to pos:', pos - scriptStart);
console.log(html.slice(scriptStart, scriptStart + 200));
