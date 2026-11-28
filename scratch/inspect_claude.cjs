const fs = require('fs');

const html = fs.readFileSync('scratch/claude.html', 'utf8');
const match = html.match(/<script id="__NEXT_DATA__"[^>]*>(.*?)<\/script>/s);
if (match) {
  const json = JSON.parse(match[1]);
  console.log('PageProps keys:', Object.keys(json.props?.pageProps || {}));
  
  function findKeys(obj, path = '', depth = 0) {
    if (!obj || depth > 5) return;
    for (const k of Object.keys(obj)) {
      const curPath = path ? path + '.' + k : k;
      if (k.toLowerCase().includes('chat') || k.toLowerCase().includes('message') || k.toLowerCase().includes('conversation')) {
        console.log('Found interesting path:', curPath, Array.isArray(obj[k]) ? 'Array[' + obj[k].length + ']' : typeof obj[k]);
      }
      if (typeof obj[k] === 'object' && obj[k] !== null && !Array.isArray(obj[k])) {
        findKeys(obj[k], curPath, depth + 1);
      }
    }
  }
  findKeys(json.props?.pageProps);
} else {
  console.log('No __NEXT_DATA__ found');
}
