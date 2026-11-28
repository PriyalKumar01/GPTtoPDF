const fs = require('fs');

async function inspectBoth() {
  try {
    const geminiUrl = 'https://share.gemini.google/SwWQ6ETSurNF';
    const geminiRes = await fetch(geminiUrl, {
      redirect: 'follow',
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36' }
    });
    console.log('Gemini final url:', geminiRes.url, 'status:', geminiRes.status);
    const geminiHtml = await geminiRes.text();
    fs.writeFileSync('scratch/gemini.html', geminiHtml);
    console.log('Gemini HTML saved, bytes:', geminiHtml.length);

    // Search for message text in HTML
    const matchData = geminiHtml.match(/data-initial-data="([^"]+)"/);
    if (matchData) {
      console.log('Found data-initial-data, length:', matchData[1].length);
    }
  } catch (e) {
    console.error('Gemini error:', e.message);
  }

  try {
    const claudeUrl = 'https://claude.ai/share/d7e8eab2-f687-42d8-b9d7-0ae3915ac279';
    const claudeRes = await fetch(claudeUrl, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36' }
    });
    console.log('Claude status:', claudeRes.status);
    const claudeHtml = await claudeRes.text();
    fs.writeFileSync('scratch/claude.html', claudeHtml);
    console.log('Claude HTML saved, bytes:', claudeHtml.length);
  } catch (e) {
    console.error('Claude error:', e.message);
  }
}

inspectBoth();
