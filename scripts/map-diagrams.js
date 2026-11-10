async function mapDiagrams() {
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
  const safeUrls = data.safe_urls || [];
  const imageUrls = safeUrls.filter(u => u.includes('images.openai.com'));
  const text = data.linear_conversation[20].message.content.parts[0];

  // Find all <AsyncImage query="..." /> in text
  const asyncImgMatches = [...text.matchAll(/<AsyncImage\s+query="([^"]+)"/g)];
  console.log(`Found ${asyncImgMatches.length} <AsyncImage> tags in text:`);
  asyncImgMatches.forEach((m, i) => {
    console.log(`  [${i + 1}] Query: "${m[1]}"`);
    console.log(`       Mapped to Image: ${imageUrls[i % imageUrls.length]?.slice(0, 70)}...`);
  });
}

mapDiagrams();
