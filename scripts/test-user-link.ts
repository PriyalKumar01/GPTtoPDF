import { NotesPdfGenerator } from '../lib/pdf-generator';
import { APP_CONFIG } from '../lib/config';

async function testUserLink() {
  const shareId = '6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  const url = `https://chatgpt.com/backend-api/share/${shareId}`;
  console.log('Fetching ChatGPT share API:', url);
  const res = await fetch(url, {
    headers: {
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
      Accept: 'application/json',
    },
  });

  const data = await res.json();
  console.log('Got response. Title:', data.title);

  const messages: any[] = [];
  if (data.linear_conversation) {
    for (const item of data.linear_conversation) {
      const msg = item.message;
      if (!msg) continue;
      const role = msg.author?.role;
      if (role !== 'user' && role !== 'assistant') continue;

      let text = '';
      if (Array.isArray(msg.content?.parts)) {
        for (const p of msg.content.parts) {
          if (typeof p === 'string') {
            text += (text ? '\n\n' : '') + p;
          } else if (p && p.content_type === 'image_asset_pointer') {
            text += (text ? '\n\n' : '') + `📷 [Attached Image / Question Paper: ${p.width}x${p.height}]`;
          }
        }
      } else if (typeof msg.content?.text === 'string') {
        text = msg.content.text;
      }

      // Clean JSX / DIL widget markup from ChatGPT
      text = text
        .replace(/\{@body[\s\S]*?\}/g, '')
        .replace(/<\/?(?:grid|grid-item|AsyncImage|box|row|badge|checkbox|button)[^>]*>/gi, '')
        .trim();

      if (text) {
        messages.push({
          id: msg.id || `msg-${messages.length + 1}`,
          role,
          authorName: role === 'user' ? 'You' : 'ChatGPT',
          content: text,
        });
      }
    }
  }

  console.log('Parsed messages count:', messages.length);
  for (const m of messages) {
    console.log(`- ${m.authorName}: ${m.content.slice(0, 100)}... (Length: ${m.content.length})`);
  }

  const conversation = {
    id: shareId,
    title: data.title || 'Software Testing Preparation',
    sourceUrl: `https://chatgpt.com/share/${shareId}`,
    createdAt: new Date().toISOString(),
    messages,
    stats: {
      userMessages: messages.filter((m) => m.role === 'user').length,
      assistantMessages: messages.filter((m) => m.role === 'assistant').length,
      totalMessages: messages.length,
      estimatedPages: Math.ceil(messages.reduce((acc, m) => acc + m.content.length, 0) / 1600),
    },
  };

  const generator = new NotesPdfGenerator(conversation as any, APP_CONFIG.defaultPdfOptions);
  const doc = generator.generate();
  console.log('PDF Generated successfully! Total pages:', doc.getNumberOfPages());
  const buf = doc.output('arraybuffer');
  console.log('PDF Buffer Size:', buf.byteLength, 'bytes');
}

testUserLink().catch(console.error);
