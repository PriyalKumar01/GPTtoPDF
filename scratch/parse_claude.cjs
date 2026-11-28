const fs = require('fs');
const html = fs.readFileSync('scratch/claude_dump.html', 'utf8');

// Check __NEXT_DATA__
const nextMatch = html.match(/<script id="__NEXT_DATA__"[^>]*>([\s\S]*?)<\/script>/);
if (nextMatch) {
  console.log('__NEXT_DATA__ found! Length:', nextMatch[1].length);
  try {
    const json = JSON.parse(nextMatch[1]);
    console.log('Keys in json:', Object.keys(json));
    console.log('Keys in pageProps:', Object.keys(json.props?.pageProps || {}));
    fs.writeFileSync('scratch/claude_pageprops.json', JSON.stringify(json.props?.pageProps, null, 2));
  } catch(e) {
    console.error('Parse error:', e);
  }
} else {
  console.log('No __NEXT_DATA__ found in Claude dump');
}
