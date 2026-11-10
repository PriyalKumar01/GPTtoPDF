import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

async function testFullDiagramWorkflow() {
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
  const rawText = data.linear_conversation[20].message.content.parts[0];

  console.log(`Extracting diagrams and converting to base64 data URLs...`);
  // Download top 5 diagrams in parallel
  const imageMap = new Map<string, string>();
  const topImages = imageUrls.slice(0, 10);

  await Promise.all(
    topImages.map(async (imgUrl: string, idx: number) => {
      try {
        const imgRes = await fetch(imgUrl);
        if (imgRes.ok) {
          const ab = await imgRes.arrayBuffer();
          const b64 = Buffer.from(ab).toString('base64');
          imageMap.set(imgUrl, `data:image/jpeg;base64,${b64}`);
        }
      } catch (e) {
        console.warn('Failed to fetch image:', imgUrl);
      }
    })
  );

  console.log(`Successfully downloaded ${imageMap.size} diagram images!`);

  // Map <AsyncImage query="..." /> to markdown image with base64 dataUrl
  let imgIndex = 0;
  const processedText = rawText.replace(
    /<AsyncImage\s+query="([^"]+)"[^>]*\/>/g,
    (match: string, query: string) => {
      const assignedUrl = imageUrls[imgIndex % imageUrls.length];
      imgIndex++;
      const dataUrl = imageMap.get(assignedUrl) || assignedUrl;
      return `\n\n![Diagram: ${query}](${dataUrl})\n\n`;
    }
  );

  // Clean XML tags like <text color="secondary" size="xs">...</text> -> *...*
  const cleanedText = processedText
    .replace(/<badge[^>]*>(.*?)<\/badge>/gi, '($1) ')
    .replace(/<text[^>]*>(.*?)<\/text>/gi, '\n_$1_\n')
    .replace(/\{@body[\s\S]*?\}/g, '')
    .replace(/<\/?(?:grid|grid-item|box|row|badge|checkbox|button)[^>]*>/gi, '')
    .replace(/[\uD800-\uDBFF][\uDC00-\uDFFF]/g, '')
    .replace(/\r\n/g, '\n')
    .trim();

  // Test snippet around "Important diagrams to practise"
  const diagIdx = cleanedText.indexOf('Important diagrams to practise');
  console.log('\n--- CLEANED SNIPPET AROUND SECTION 6 ---');
  console.log(cleanedText.slice(diagIdx, diagIdx + 700));

  // Test snippet around Q13
  const q13Idx = cleanedText.indexOf('Explain the Regression Testing process with a suitable diagram');
  console.log('\n--- CLEANED SNIPPET AROUND Q13 ---');
  console.log(cleanedText.slice(q13Idx, q13Idx + 600));
}

testFullDiagramWorkflow().catch(console.error);
