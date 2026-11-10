import fs from 'fs';
import { extractAiConversation } from '../lib/extract-ai';
import { NotesPdfGenerator } from '../lib/pdf-generator';
import { APP_CONFIG } from '../lib/config';

async function debugGen() {
  const url = 'https://chatgpt.com/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  const res = await extractAiConversation(url);
  if (!res.conversation) return;

  const content = res.conversation.messages[1].content;
  const lines = content.split('\n');
  console.log('Total lines in Msg 1:', lines.length);
  for (let idx = 0; idx < 20; idx++) {
    console.log(`Line ${idx}: [${lines[idx].slice(0, 60)}]`);
  }
}

debugGen().catch(console.error);
