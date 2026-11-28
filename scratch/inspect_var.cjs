const fs = require('fs');
const s = fs.readFileSync('scratch/slice.txt', 'utf8');
// Find what data structure this is: find the preceding variable name
const html = fs.readFileSync('gemini_dump.html', 'utf8');
const pos = 775460;
const start = Math.max(0, pos - 2000);
console.log(html.slice(start, start + 500));
