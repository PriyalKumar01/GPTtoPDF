const fs = require('fs');
const lines = fs.readFileSync('C:/Users/priya/.gemini/antigravity/brain/38cef4fa-a925-4e51-a50e-ec957c7d198b/.system_generated/logs/transcript.jsonl', 'utf8').split('\n');
lines.forEach(l => {
  if (!l) return;
  try {
    const o = JSON.parse(l);
    if (o.tool_calls) {
      for (const tc of o.tool_calls) {
        if (tc.name === 'send_message') {
          console.log('--- SUBAGENT SENT MESSAGE ---');
          console.log(tc.args.Message);
        }
      }
    }
  } catch(e) {}
});
