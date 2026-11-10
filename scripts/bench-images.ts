async function benchmark() {
  const start = Date.now();
  const res = await fetch('https://chatgpt.com/backend-api/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8');
  const d = await res.json();
  const imgs = (d.safe_urls || []).filter((u: string) => u.includes('images.openai.com'));
  console.log('Downloading', imgs.length, 'images in parallel...');
  const results = await Promise.all(
    imgs.map(async (url: string) => {
      try {
        const r = await fetch(url);
        const b = await r.arrayBuffer();
        return b.byteLength;
      } catch (e) {
        return 0;
      }
    })
  );
  console.log('Downloaded', results.filter(s => s > 0).length, 'images in', Date.now() - start, 'ms');
}

benchmark().catch(console.error);
