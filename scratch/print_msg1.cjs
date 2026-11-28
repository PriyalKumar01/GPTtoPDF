const fs = require('fs');
const txt = JSON.parse(fs.readFileSync('scratch/subagent_msg_1.txt', 'utf8'));
console.log(txt.slice(1000, 3000));
