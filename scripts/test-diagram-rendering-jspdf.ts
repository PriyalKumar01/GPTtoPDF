import { jsPDF } from 'jspdf';
import fs from 'fs';

async function testDiagramRendering() {
  const shareId = '6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  const url = `https://chatgpt.com/backend-api/share/${shareId}`;
  const res = await fetch(url);
  const data = await res.json();
  const safeUrls = data.safe_urls || [];
  const imageUrls = safeUrls.filter((u: string) => u.includes('images.openai.com'));

  console.log(`Found ${imageUrls.length} OpenAI images`);

  // Download first 4 images (the 4 diagrams from section 6)
  const imageBuffers = await Promise.all(
    imageUrls.slice(0, 4).map(async (u: string) => {
      const r = await fetch(u);
      const ab = await r.arrayBuffer();
      return `data:image/jpeg;base64,${Buffer.from(ab).toString('base64')}`;
    })
  );

  const doc = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4',
  });

  const pageWidth = 210;
  const pageHeight = 297;
  const marginX = 14;
  const contentWidth = pageWidth - marginX * 2;
  let currentY = 20;

  doc.setFont('helvetica', 'bold');
  doc.setFontSize(14);
  doc.text('Important diagrams to practise', marginX, currentY);
  currentY += 8;

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  doc.setTextColor(71, 85, 105);
  doc.text(
    'Diagrams can make your 5-mark answers more presentable and help explain the testing process.',
    marginX,
    currentY
  );
  currentY += 8;

  const titles = [
    '1. Integration Testing - Top-down and bottom-up approaches',
    '2. Testing Levels - Unit, integration, system and acceptance testing',
    '3. Regression Testing - Change analysis to retesting',
    '4. Testing Life Cycle - Planning, designing, execution and reporting',
  ];

  for (let i = 0; i < imageBuffers.length; i++) {
    const b64 = imageBuffers[i];
    const props = doc.getImageProperties(b64);
    const aspect = props.width / props.height;
    const maxW = 110;
    const maxH = 50;
    let w = maxW;
    let h = w / aspect;
    if (h > maxH) {
      h = maxH;
      w = h * aspect;
    }

    if (currentY + h + 15 > pageHeight - 15) {
      doc.addPage();
      currentY = 20;
    }

    // Title above
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.setTextColor(30, 41, 59);
    doc.text(titles[i], marginX, currentY);
    currentY += 4;

    // Image border & image
    const imgX = marginX + (contentWidth - w) / 2;
    doc.setFillColor(255, 255, 255);
    doc.setDrawColor(226, 232, 240);
    doc.setLineWidth(0.3);
    doc.roundedRect(imgX - 1, currentY - 1, w + 2, h + 2, 1, 1, 'FD');
    doc.addImage(b64, 'JPEG', imgX, currentY, w, h);
    currentY += h + 8;
  }

  const out = doc.output('arraybuffer');
  fs.writeFileSync('scratch/diagram_test.pdf', Buffer.from(out));
  console.log('Saved scratch/diagram_test.pdf, size:', out.byteLength, 'bytes, pages:', doc.getNumberOfPages());
}

testDiagramRendering().catch(console.error);
