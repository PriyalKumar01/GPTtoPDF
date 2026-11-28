const fs = require('fs');
const txt = fs.readFileSync('scratch/subagent_msg_1.txt', 'utf8');
console.log(txt.slice(2500, 4500));
