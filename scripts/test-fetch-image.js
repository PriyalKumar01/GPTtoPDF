async function testFetchImage() {
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
  const imageUrls = data.safe_urls.filter(u => u.includes('images.openai.com') || u.includes('.png') || u.includes('.jpg'));
  console.log('Total image URLs in safe_urls:', imageUrls.length);

  for (let i = 0; i < Math.min(3, imageUrls.length); i++) {
    const imgUrl = imageUrls[i];
    console.log(`Testing image ${i + 1}:`, imgUrl.slice(0, 80));
    try {
      const imgRes = await fetch(imgUrl, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        },
      });
      console.log(`Status: ${imgRes.status}, Content-Type: ${imgRes.headers.get('content-type')}`);
      const buf = await imgRes.arrayBuffer();
      console.log(`Bytes downloaded: ${buf.byteLength}`);
    } catch (e) {
      console.error('Fetch error:', e);
    }
  }
}

testFetchImage();
