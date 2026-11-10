import { jsPDF } from 'jspdf';
import fs from 'fs';

async function testDiagramEmbedding() {
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
  const imageUrls = safeUrls.filter((u: string) => u.includes('images.openai.com'));
  console.log(`Found ${imageUrls.length} diagram images.`);

  if (imageUrls.length > 0) {
    console.log('Downloading first diagram...');
    const imgRes = await fetch(imageUrls[0]);
    const arrayBuffer = await imgRes.arrayBuffer();
    const base64 = Buffer.from(arrayBuffer).toString('base64');
    const dataUrl = `data:image/jpeg;base64,${base64}`;

    const doc = new jsPDF();
    doc.text('Testing Diagram Embedding in PDF:', 15, 20);
    // embed image: x=15, y=30, width=120, height=75
    doc.addImage(dataUrl, 'JPEG', 15, 30, 120, 75);
    doc.text('Diagram embedded successfully!', 15, 115);

    const buf = doc.output('arraybuffer');
    console.log(`PDF generated with diagram! Bytes: ${buf.byteLength}`);
  }
}

testDiagramEmbedding().catch(console.error);
