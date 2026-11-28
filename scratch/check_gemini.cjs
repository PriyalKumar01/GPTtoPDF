const fs = require('fs');
const html = fs.readFileSync('gemini_dump.html', 'utf8');
const match = html.match(/<script id="bard-initial-data"[^>]*data-payload="([^"]*)"/);
if (match) {
  const decoded = match[1]
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');
  console.log('Decoded payload:', decoded);
} else {
  console.log('No bard-initial-data match');
}
