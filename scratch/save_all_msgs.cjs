const fs = require('fs');
const lines = fs.readFileSync('C:/Users/priya/.gemini/antigravity/brain/38cef4fa-a925-4e51-a50e-ec957c7d198b/.system_generated/logs/transcript.jsonl', 'utf8').split('\n');
let i = 0;
for (const l of lines) {
  if (!l) continue;
  try {
    const o = JSON.parse(l);
    if (o.tool_calls) {
      for (const tc of o.tool_calls) {
        if (tc.name === 'send_message') {
          i++;
          fs.writeFileSync(`scratch/subagent_msg_${i}.txt`, tc.args.Message);
        }
      }
    }
  } catch(e) {}
}
console.log('Total messages:', i);
