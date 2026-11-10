async function checkDiagrams() {
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
  const text = data.linear_conversation[20].message.content.parts[0];

  console.log('Safe URLs in data:', data.safe_urls?.length);
  if (data.safe_urls) {
    console.log('First 5 safe_urls:', data.safe_urls.slice(0, 5));
  }

  // Look for "Important diagrams to practise"
  const diagIdx = text.indexOf('Important diagrams to practise');
  console.log('Found "Important diagrams to practise" at index:', diagIdx);
  if (diagIdx !== -1) {
    console.log('--- SNIPPET AROUND DIAGRAMS ---');
    console.log(text.slice(diagIdx, diagIdx + 1500));
  }

  // Look for "Q13. Explain the Regression Testing process with a suitable diagram"
  const q13Idx = text.indexOf('Explain the Regression Testing process with a suitable diagram');
  console.log('Found Q13 at index:', q13Idx);
  if (q13Idx !== -1) {
    console.log('--- SNIPPET AROUND Q13 ---');
    console.log(text.slice(q13Idx, q13Idx + 1200));
  }
}

checkDiagrams();
