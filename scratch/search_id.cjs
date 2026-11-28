const fs = require('fs');
const html = fs.readFileSync('gemini_dump.html', 'utf8');
const id = '390eff5f2f16';
let pos = 0;
while ((pos = html.indexOf(id, pos)) !== -1) {
  console.log('Found 390eff5f2f16 at', pos);
  console.log(html.slice(Math.max(0, pos - 150), pos + 300));
  pos += id.length;
}
