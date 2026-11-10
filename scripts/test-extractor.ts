import { extractChatGptConversation, isValidChatGptUrl } from '../lib/extract-chatgpt';

async function testExtractor() {
  console.log('Testing URL validation...');
  console.log('Valid https://chatgpt.com/share/672fc10a-8d14-8005-9271-e945c873f1d2:', isValidChatGptUrl('https://chatgpt.com/share/672fc10a-8d14-8005-9271-e945c873f1d2'));
  console.log('Valid https://chat.openai.com/share/672fc10a-8d14-8005-9271-e945c873f1d2:', isValidChatGptUrl('https://chat.openai.com/share/672fc10a-8d14-8005-9271-e945c873f1d2'));
  console.log('Invalid evil URL:', isValidChatGptUrl('https://evil.com/share/123'));

  console.log('\nTesting extraction of sample notes...');
  const res = await extractChatGptConversation('https://chatgpt.com/share/os-exam-notes-2024');
  console.log('Sample Extraction Success:', res.success);
  if (res.conversation) {
    console.log('Title:', res.conversation.title);
    console.log('Message Count:', res.conversation.messages.length);
    console.log('Estimated Pages:', res.conversation.stats.estimatedPages);
  }
}

testExtractor().catch(console.error);
