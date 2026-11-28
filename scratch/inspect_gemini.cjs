const fs = require('fs');
const html = fs.readFileSync('gemini_dump.html', 'utf8');

console.log('Includes 390eff5f2f16:', html.includes('390eff5f2f16'));
console.log('Includes TJDQpgdxMpq9:', html.includes('TJDQpgdxMpq9'));

// Check what scripts exist after head
const scripts = html.split('<script');
console.log('Total script segments:', scripts.length);

for (let i = 0; i < scripts.length; i++) {
  const s = scripts[i];
  if (s.includes('390eff5f2f16') || s.includes('TJDQpgdxMpq9') || s.includes('c_')) {
    console.log('Script', i, 'has share id or c_! Length:', s.length);
    console.log(s.slice(0, 300));
  }
}
