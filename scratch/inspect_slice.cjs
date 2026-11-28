const fs = require('fs');
const html = fs.readFileSync('gemini_dump.html', 'utf8');
const slice = html.slice(760000, 776000);
console.log('Length:', slice.length);
fs.writeFileSync('scratch/slice.txt', slice);
console.log('Saved to scratch/slice.txt');
