import fs from 'fs';
import { extractAiConversation } from '../lib/extract-ai';

async function inspectMessages() {
  const url = 'https://chatgpt.com/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  const res = await extractAiConversation(url);
  if (!res.conversation) return;
  console.log('Messages count:', res.conversation.messages.length);
  res.conversation.messages.forEach((m, idx) => {
    console.log(`Msg ${idx}: role=${m.role}, len=${m.content.length}, preview=${m.content.slice(0, 100).replace(/\n/g, ' ')}`);
  });
}

inspectMessages().catch(console.error);
