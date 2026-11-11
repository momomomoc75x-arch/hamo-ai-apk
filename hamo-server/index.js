const express = require('express');
const cors = require('cors');

const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json());

let messages = [];

app.get('/', (req, res) => {
  res.json({
    name: 'Hamo Server',
    status: 'online',
    version: '1.0.0'
  });
});

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.post('/chat', (req, res) => {
  const message = String(req.body.message || '').trim();

  if (!message) {
    return res.status(400).json({
      error: 'اكتب رسالة أولاً'
    });
  }

  messages.push({
    message,
    time: new Date().toISOString()
  });

  res.json({
    reply: 'وصلت رسالتك إلى سيرفر حمو بنجاح: ' + message,
    server: 'Hamo Server'
  });
});

app.get('/messages', (req, res) => {
  res.json(messages);
});

app.listen(PORT, '0.0.0.0', () => {
  console.log('Hamo Server is running on port ' + PORT);
});
