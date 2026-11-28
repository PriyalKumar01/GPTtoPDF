const fs = require('fs');
const full = fs.readFileSync('C:/Users/priya/.gemini/antigravity/brain/38cef4fa-a925-4e51-a50e-ec957c7d198b/.system_generated/logs/transcript_full.jsonl', 'utf8').split('\n');
for (const l of full) {
  if (!l) continue;
  try {
    const o = JSON.parse(l);
    if (o.tool_calls) {
      for (const tc of o.tool_calls) {
        if (tc.name === 'send_message') {
          console.log('Found full message!');
          fs.writeFileSync('scratch/full_msg_1.txt', tc.args.Message);
        }
      }
    }
  } catch(e) {}
}
