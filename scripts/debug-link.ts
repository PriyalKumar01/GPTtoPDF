import fs from 'fs';

async function test() {
  const url = 'https://chatgpt.com/share/6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
  console.log('Fetching:', url);
  const headers = {
    'User-Agent':
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36',
    Accept:
      'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.9',
  };

  try {
    const res = await fetch(url, { headers });
    console.log('HTTP Status:', res.status);
    console.log('Headers:', Object.fromEntries(res.headers.entries()));
    const text = await res.text();
    console.log('Length:', text.length);
    fs.writeFileSync('scratch/chatgpt_response.html', text);
    console.log('Saved response to scratch/chatgpt_response.html');

    // Also check backend api
    const shareId = '6ae78cd4-d9e4-83e8-bb1e-0cd39b94afe8';
    console.log('Trying backend api endpoint...');
    const apiRes = await fetch(`https://chatgpt.com/backend-api/share/${shareId}`, {
      headers: {
        'User-Agent': headers['User-Agent'],
        Accept: 'application/json',
      },
    });
    console.log('API Status:', apiRes.status);
    const apiText = await apiRes.text();
    console.log('API Length:', apiText.length);
    fs.writeFileSync('scratch/chatgpt_api_response.json', apiText.slice(0, 10000));
  } catch (err) {
    console.error('Fetch error:', err);
  }
}

test();
