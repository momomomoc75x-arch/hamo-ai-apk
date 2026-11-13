const express = require('express');
const cors = require('cors');

const app = express();
const PORT = Number(process.env.PORT) || 3000;
const OLLAMA_URL = 'http://127.0.0.1:11434/api/chat';
const DEFAULT_MODEL = process.env.OLLAMA_MODEL || 'qwen2.5:7b';

app.use(cors());
app.use(express.json());

let messages = [];

async function askOllama(prompt, modelName = DEFAULT_MODEL) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 300000);

  try {
    const response = await fetch(OLLAMA_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: modelName,
        messages: [
          {
            role: 'system',
            content: 'أجب بلغة المستخدم. إذا كتب بالعربية فأجب بالعربية المصرية بوضوح واختصار، ولا تستخدم الصينية أو لغة أخرى إلا إذا طلب المستخدم ذلك.'
          },
          { role: 'user', content: prompt }
        ],
        stream: false,
        options: { temperature: 0.7, num_predict: 256 }
      }),
      signal: controller.signal
    });

    if (!response.ok) {
      throw new Error(`Ollama HTTP ${response.status}`);
    }

    const data = await response.json();
    const reply = data.message?.content || data.response || 'لم يتم الحصول على رد من النموذج.';
    return reply.trim();
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new Error('انتهت مهلة الاتصال بنموذج Ollama المحلي.');
    }
    throw new Error('تعذر الاتصال بـ Ollama محلياً: ' + (error.message || String(error)));
  } finally {
    clearTimeout(timeout);
  }
}

app.get('/', (req, res) => {
  res.json({
    name: 'Hamo Server',
    status: 'online',
    version: '1.0.0',
    provider: 'ollama-local',
    model: DEFAULT_MODEL
  });
});

app.get('/health', async (req, res) => {
  try {
    const response = await fetch('http://127.0.0.1:11434/api/tags');
    if (!response.ok) throw new Error('Ollama is unavailable');
    const data = await response.json();
    const available = (data.models || []).some(model => model.name === DEFAULT_MODEL);
    if (!available) {
      return res.status(503).json({ status: 'model-missing', provider: 'ollama-local', model: DEFAULT_MODEL });
    }
    return res.json({ status: 'ok', provider: 'ollama-local', model: DEFAULT_MODEL });
  } catch (error) {
    return res.status(503).json({ status: 'unavailable', provider: 'ollama-local', error: error.message });
  }
});

app.post('/chat', async (req, res) => {
  const message = String(req.body.message || '').trim();

  if (!message) {
    return res.status(400).json({ error: 'اكتب رسالة أولاً' });
  }

  const entry = { message, time: new Date().toISOString() };
  messages.push(entry);

  try {
    const model = req.body.model || DEFAULT_MODEL;
    const reply = await askOllama(message, model);
    return res.json({
      reply,
      server: 'Hamo Server',
      provider: 'ollama-local',
      model
    });
  } catch (error) {
    return res.status(502).json({
      error: error.message,
      server: 'Hamo Server',
      provider: 'ollama-local',
      fallback: 'استخدم نموذج Ollama محلياً أو تأكد من تشغيل خدمة Ollama على localhost:11434'
    });
  }
});

app.get('/messages', (req, res) => {
  res.json(messages);
});

app.listen(PORT, '0.0.0.0', () => {
  console.log('Hamo Server is running on port ' + PORT + ' with Ollama local provider');
});
