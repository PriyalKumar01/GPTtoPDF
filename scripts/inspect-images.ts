import fs from 'fs';

async function inspect() {
  const res = await fetch('https://chatgpt.com/backend-api/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8');
  const d = await res.json();
  console.log('Title:', d.title);
  console.log('safe_urls count:', d.safe_urls?.length);
  const openAiImgs = (d.safe_urls || []).filter((u: string) => u.includes('images.openai.com'));
  console.log('openAiImgs count:', openAiImgs.length);
  for (let i = 0; i < Math.min(openAiImgs.length, 5); i++) {
    console.log(`Image ${i}:`, openAiImgs[i].slice(0, 80));
  }

  // Check each message in linear_conversation
  let totalAsyncImgs = 0;
  for (let idx = 0; idx < d.linear_conversation.length; idx++) {
    const item = d.linear_conversation[idx];
    const role = item.message?.author?.role;
    const parts = item.message?.content?.parts || [];
    for (const part of parts) {
      if (typeof part === 'string') {
        const matches = [...part.matchAll(/<AsyncImage\s+query="([^"]+)"[^>]*\/>/g)];
        if (matches.length > 0) {
          console.log(`Msg ${idx} (${role}) has ${matches.length} AsyncImages:`, matches.map(m => m[1]));
          totalAsyncImgs += matches.length;
        }
      }
    }
  }
  console.log('Total AsyncImages found:', totalAsyncImgs);
}

inspect().catch(console.error);
