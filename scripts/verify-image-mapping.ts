async function verifyImageMapping() {
  const res = await fetch('https://chatgpt.com/backend-api/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8');
  const d = await res.json();
  const safeUrls = (d.safe_urls || []).filter((u: string) => u.includes('images.openai.com'));
  
  let count = 0;
  for (const item of d.linear_conversation) {
    const parts = item.message?.content?.parts || [];
    for (const part of parts) {
      if (typeof part === 'string') {
        const matches = [...part.matchAll(/<AsyncImage\s+query="([^"]+)"/g)];
        for (const m of matches) {
          console.log(`Image #${count + 1}: ${m[1]}`);
          console.log(`  -> URL: ${safeUrls[count]?.slice(0, 70)}`);
          count++;
        }
      }
    }
  }
}

verifyImageMapping().catch(console.error);
