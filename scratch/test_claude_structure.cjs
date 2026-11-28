const fs = require('fs');

async function testClaudeStructure() {
  const res = await fetch('https://claude.ai/api/chat_snapshots/d7e8eab2-f687-42d8-b9d7-0ae3915ac279', {
    headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
  });
  const data = await res.json();
  console.log('Snapshot Name:', data.snapshot_name);
  console.log('Created By:', data.created_by);
  console.log('Messages count:', data.chat_messages?.length);
  data.chat_messages?.forEach((m, i) => {
    console.log(`\n--- Message ${i + 1} (${m.sender}) ---`);
    console.log('Text preview:', (m.text || '').substring(0, 100));
    if (m.content && Array.isArray(m.content)) {
      console.log('Content types:', m.content.map(c => c.type));
    }
    if (m.attachments && m.attachments.length > 0) {
      console.log('Attachments:', m.attachments);
    }
  });
}

testClaudeStructure();
