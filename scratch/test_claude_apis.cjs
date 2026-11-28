const endpoints = [
  'https://claude.ai/api/share/099a5e42-f687-42d8-b9d7-0ae3915ac279',
  'https://claude.ai/api/shared_conversations/099a5e42-f687-42d8-b9d7-0ae3915ac279',
  'https://claude.ai/api/shares/099a5e42-f687-42d8-b9d7-0ae3915ac279',
  'https://claude.ai/api/published_conversations/099a5e42-f687-42d8-b9d7-0ae3915ac279',
  'https://claude.ai/api/public/conversations/099a5e42-f687-42d8-b9d7-0ae3915ac279',
  'https://claude.ai/api/organizations/share/099a5e42-f687-42d8-b9d7-0ae3915ac279'
];

async function test() {
  for (const ep of endpoints) {
    try {
      const res = await fetch(ep, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
          'Accept': 'application/json'
        }
      });
      console.log(ep, '=> Status:', res.status);
      if (res.status === 200) {
        const text = await res.text();
        console.log('200 Response preview:', text.slice(0, 300));
      }
    } catch(e) {
      console.log(ep, '=> Error:', e.message);
    }
  }
}
test();
