import { extractAiConversation } from '../lib/extract-ai';

async function test() {
  const url = 'https://chatgpt.com/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  console.log('Testing extraction with extractAiConversation...');
  const res = await extractAiConversation(url);
  console.log('Success:', res.success);
  if (res.conversation) {
    console.log('Title:', res.conversation.title);
    console.log('Messages count:', res.conversation.messages.length);
    console.log('Est pages:', res.conversation.stats.estimatedPages);
  } else {
    console.error('Error:', res.error);
  }
}

test().catch(console.error);
