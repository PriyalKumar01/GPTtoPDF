import fs from 'fs';

const raw = fs.readFileSync('scratch/chatgpt_api_response.json', 'utf-8');
const data = JSON.parse(raw);

console.log('Total items in linear_conversation:', data.linear_conversation.length);
data.linear_conversation.forEach((item, idx) => {
  const msg = item.message;
  if (!msg) {
    console.log(`[${idx}] No message`);
    return;
  }
  const role = msg.author?.role;
  const contentType = msg.content?.content_type;
  const recipient = msg.recipient;
  let text = '';
  if (Array.isArray(msg.content?.parts)) {
    text = msg.content.parts.filter(p => typeof p === 'string').join(' ').slice(0, 80);
  } else if (msg.content?.text) {
    text = msg.content.text.slice(0, 80);
  }
  console.log(`[${idx}] role=${role}, contentType=${contentType}, recipient=${recipient}, length=${text.length}, preview="${text.replace(/\n/g, ' ')}"`);
});
