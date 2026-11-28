const fs = require('fs');
const html = fs.readFileSync('scratch/claude_dump.html', 'utf8');
const scriptSrcs = [...html.matchAll(/src="([^"]+)"/g)].map(m => m[1]);
console.log('Script srcs:', scriptSrcs);
const packMatch = html.match(/id="pack-files"[^>]*>([^<]+)<\/script>/);
if (packMatch) {
  console.log('Pack files:', JSON.parse(packMatch[1]));
}
