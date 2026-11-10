import fs from 'fs';

async function checkSection6() {
  const res = await fetch('https://chatgpt.com/backend-api/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8');
  const d = await res.json();
  const rawText = d.linear_conversation[20].message.content.parts[0];
  
  const idx6 = rawText.indexOf('Important diagrams to practise');
  console.log('--- SECTION 6 RAW (1200 chars) ---');
  console.log(rawText.slice(idx6, idx6 + 1200));

  const idx13 = rawText.indexOf('Explain the Regression Testing process with a suitable diagram');
  console.log('--- Q13 RAW (1000 chars) ---');
  console.log(rawText.slice(idx13, idx13 + 1000));
}

checkSection6().catch(console.error);
