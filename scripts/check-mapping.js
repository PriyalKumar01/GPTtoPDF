async function checkAsyncImageMapping() {
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
  const msg = data.linear_conversation[20].message;
  console.log('Message metadata keys:', Object.keys(msg.metadata || {}));
  if (msg.metadata?.content_references) {
    console.log('Content references count:', msg.metadata.content_references.length);
    console.log('First 3 references:', JSON.stringify(msg.metadata.content_references.slice(0, 3), null, 2));
  }
}

checkAsyncImageMapping();
