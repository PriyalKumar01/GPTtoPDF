async function testCleanedExtraction() {
  const shareId = '6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  const url = `https://chatgpt.com/backend-api/share/${shareId}`;
  const res = await fetch(url, {
    headers: {
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
      Accept: 'application/json',
    },
  });
  const data = await res.json();

  const messages = [];

  for (const item of data.linear_conversation) {
    const msg = item.message;
    if (!msg) continue;
    const role = msg.author?.role;
    const contentType = msg.content?.content_type;
    const isHidden = msg.metadata?.is_visually_hidden_from_conversation;

    // Filter out system, tool, hidden drafts, internal python code, and thoughts
    if (role !== 'user' && role !== 'assistant') continue;
    if (isHidden === true) continue;
    if (contentType === 'code' || contentType === 'thoughts' || contentType === 'reasoning_recap' || contentType === 'model_editable_context') {
      continue;
    }

    let text = '';
    if (Array.isArray(msg.content?.parts)) {
      // For user multimodal or assistant, extract ONLY valid string text parts
      const stringParts = msg.content.parts.filter(p => typeof p === 'string' && p.trim().length > 0);
      text = stringParts.join('\n\n');
    } else if (typeof msg.content?.text === 'string') {
      text = msg.content.text;
    }

    // Clean XML/DIL tags:
    // e.g. <badge color="success">Units 3 & 4</badge> -> (Units 3 & 4)
    text = text
      .replace(/<badge[^>]*>(.*?)<\/badge>/gi, '($1) ')
      .replace(/\{@body[\s\S]*?\}/g, '')
      .replace(/<\/?(?:grid|grid-item|AsyncImage|box|row|badge|checkbox|button)[^>]*>/gi, '')
      .replace(/\r\n/g, '\n')
      .trim();

    if (text) {
      messages.push({
        role,
        authorName: role === 'user' ? 'USER' : 'ASSISTANT',
        content: text,
      });
    }
  }

  console.log('Total extracted clean messages:', messages.length);
  messages.forEach((m, idx) => {
    console.log(`[Message ${idx + 1}] ${m.authorName} (${m.content.length} chars):`);
    console.log(m.content.slice(0, 200) + '...\n');
  });
}

testCleanedExtraction();
