import fs from 'fs';
import { extractAiConversation } from '../lib/extract-ai';
import { NotesPdfGenerator } from '../lib/pdf-generator';
import { APP_CONFIG } from '../lib/config';

async function testPipeline() {
  const url = 'https://chatgpt.com/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  console.log('Extracting conversation with diagrams from:', url);

  const start = Date.now();
  const res = await extractAiConversation(url);
  console.log(`Extraction took ${Date.now() - start} ms, success:`, res.success);

  if (!res.success || !res.conversation) {
    console.error('Extraction failed:', res.error);
    return;
  }

  console.log('Conversation Title:', res.conversation.title);
  console.log('Messages count:', res.conversation.messages.length);
  console.log('Document Stats:', res.conversation.stats);

  // Check if assistant message contains markdown images
  const asstMsg = res.conversation.messages.find(m => m.role === 'assistant');
  const imgMatches = asstMsg?.content.match(/!\[Diagram:[^\]]+\]\(data:image\/[^;]+;base64,[^)]+\)/g);
  console.log(`Found ${imgMatches?.length || 0} base64 embedded diagrams in assistant message!`);

  // Check if any <text color="secondary" size="xs"> remains
  const hasTextTag = asstMsg?.content.includes('<text');
  console.log('Contains <text tags?', hasTextTag);

  // Generate PDF
  const generator = new NotesPdfGenerator(res.conversation, {
    ...APP_CONFIG.defaultPdfOptions,
    fontSize: 'compact' as any,
  });
  const doc = generator.generate();
  const pdfBytes = doc.output('arraybuffer');
  fs.writeFileSync('scratch/full_notes_with_diagrams.pdf', Buffer.from(pdfBytes));

  console.log(`Generated PDF: scratch/full_notes_with_diagrams.pdf`);
  console.log(`Total Pages: ${doc.getNumberOfPages()}`);
  console.log(`PDF Size: ${(pdfBytes.byteLength / 1024).toFixed(1)} KB`);
}

testPipeline().catch(console.error);
