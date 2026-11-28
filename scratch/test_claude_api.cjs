async function testClaudeApis() {
  const id = 'd7e8eab2-f687-42d8-b9d7-0ae3915ac279';
  const endpoints = [
    'https://claude.ai/api/share/' + id,
    'https://claude.ai/api/shared_conversations/' + id,
    'https://claude.ai/api/shared_chats/' + id,
    'https://claude.ai/api/chat/' + id,
    'https://claude.ai/api/share_conversations/' + id,
    'https://claude.ai/api/shared/' + id,
    'https://claude.ai/api/public/conversations/' + id
  ];
  for (const ep of endpoints) {
    try {
      const res = await fetch(ep, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
          'Accept': 'application/json'
        }
      });
      console.log(ep, 'status:', res.status);
      if (res.status === 200) {
        const text = await res.text();
        console.log('200 SUCCESS! sample:', text.substring(0, 300));
      }
    } catch(e) {
      console.log(ep, 'error:', e.message);
    }
  }
}
testClaudeApis();
