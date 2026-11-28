const fs = require('fs');
const txt = fs.readFileSync('scratch/full_msg_1.txt', 'utf8');
// Print headings and selectors
const lines = txt.split('\n');
for (const line of lines) {
  if (line.startsWith('#') || line.includes('Selector') || line.includes('selector') || line.includes('Tags') || line.includes('response-container') || line.includes('model')) {
    console.log(line);
  }
}
