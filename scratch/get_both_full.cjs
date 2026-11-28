const fs = require('fs');
const full = fs.readFileSync('C:/Users/priya/.gemini/antigravity/brain/38cef4fa-a925-4e51-a50e-ec957c7d198b/.system_generated/logs/transcript_full.jsonl', 'utf8').split('\n');
let count = 0;
for (const l of full) {
  if (!l) continue;
  try {
    const o = JSON.parse(l);
    if (o.tool_calls) {
      for (const tc of o.tool_calls) {
        if (tc.name === 'send_message') {
          count++;
          console.log('Writing msg', count);
          fs.writeFileSync(`scratch/full_msg_${count}.txt`, tc.args.Message);
        }
      }
    }
  } catch(e) {}
}
