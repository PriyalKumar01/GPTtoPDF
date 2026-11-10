import { extractAiConversation } from '../lib/extract-ai';
import { NotesPdfGenerator } from '../lib/pdf-generator';
import { APP_CONFIG } from '../lib/config';

async function testFull() {
  const url = 'https://chatgpt.com/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  console.log('1. Extracting AI Conversation...');
  const res = await extractAiConversation(url);
  if (!res.success || !res.conversation) {
    console.error('Failed:', res.error);
    return;
  }

  console.log('2. Success! Title:', res.conversation.title);
  console.log('Messages extracted:', res.conversation.messages.length);
  res.conversation.messages.forEach((m, idx) => {
    console.log(`Msg ${idx + 1} [${m.authorName}]: ${m.content.slice(0, 120)}... (${m.content.length} chars)`);
  });

  console.log('3. Generating Dense Academic PDF...');
  const generator = new NotesPdfGenerator(res.conversation, APP_CONFIG.defaultPdfOptions);
  const doc = generator.generate();
  const pageCount = doc.getNumberOfPages();
  const buffer = doc.output('arraybuffer');
  console.log(`4. PDF Generation Finished! Total Pages: ${pageCount}, Bytes: ${buffer.byteLength}`);
}

testFull().catch(console.error);
