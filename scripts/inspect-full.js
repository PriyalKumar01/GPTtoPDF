async function main() {
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
  console.log('Total linear_conversation items:', data.linear_conversation.length);

  data.linear_conversation.forEach((item, idx) => {
    const msg = item.message;
    if (!msg) return;
    const role = msg.author?.role;
    const contentType = msg.content?.content_type;
    const metadata = msg.metadata || {};
    const isHidden = metadata.is_visually_hidden_from_conversation;
    const msgType = metadata.message_type;
    let text = '';
    if (Array.isArray(msg.content?.parts)) {
      text = msg.content.parts.filter(p => typeof p === 'string').join(' ').slice(0, 100);
    } else if (msg.content?.text) {
      text = msg.content.text.slice(0, 100);
    }
    console.log(`[${idx}] role=${role}, type=${contentType}, isHidden=${isHidden}, msgType=${msgType}, preview="${text.replace(/\n/g, ' ')}"`);
  });
}

main().catch(console.error);
