import fs from 'fs';
import { extractAiConversation } from '../lib/extract-ai';
import { NotesPdfGenerator } from '../lib/pdf-generator';
import { APP_CONFIG } from '../lib/config';

async function stepGen() {
  const url = 'https://chatgpt.com/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  const res = await extractAiConversation(url);
  if (!res.conversation) return;

  const generator = new NotesPdfGenerator(res.conversation, {
    ...APP_CONFIG.defaultPdfOptions,
    fontSize: 'compact' as any,
  });

  // Call private renderHeader, renderUserBox, renderAssistantContent
  const genAny = generator as any;
  genAny.renderHeader();
  console.log('After header, pages:', genAny.doc.getNumberOfPages(), 'currentY:', genAny.currentY);
  genAny.renderUserBox(res.conversation.messages[0].content);
  console.log('After user box, pages:', genAny.doc.getNumberOfPages(), 'currentY:', genAny.currentY);
  try {
    genAny.renderAssistantContent(res.conversation.messages[1].content);
    console.log('After assistant, pages:', genAny.doc.getNumberOfPages(), 'currentY:', genAny.currentY);
  } catch (e: any) {
    console.error('Crash in renderAssistantContent:', e);
  }
}

stepGen().catch(console.error);
