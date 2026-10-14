const http = require('http');
const fs = require('fs');
const path = require('path');
const { GoogleGenAI } = require('@google/genai');

// Load .env if present
const envPath = path.join(__dirname, '.env');
if (fs.existsSync(envPath)) {
  const lines = fs.readFileSync(envPath, 'utf8').split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eqIdx = trimmed.indexOf('=');
    if (eqIdx > 0) {
      const k = trimmed.slice(0, eqIdx).trim();
      const v = trimmed.slice(eqIdx + 1).trim().replace(/^["']|["']$/g, '');
      if (!process.env[k]) process.env[k] = v;
    }
  }
}

const PORT = 3000;
const MAPS_API_KEY = process.env.VITE_GOOGLE_MAPS_API_KEY || '';
const ai = new GoogleGenAI();
const HTML_FILE = path.join(__dirname, 'index.html');

const server = http.createServer((req, res) => {
  const url = (req.url || '/').split('?')[0];

  if (url === '/health' || url === '/api/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', app: 'MedTrace' }));
    return;
  }

  // Gemini Chat Endpoint with Google Maps Grounding and Multi-Language Support
  if (req.method === 'POST' && (url === '/api/chat' || url === '/api/ai-assistant')) {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body || '{}');
        const messages = payload.messages || [];
        const userLocation = payload.userLocation;
        const requestedModel = payload.model || 'gemini-3.8-flash';
        const lang = payload.language || 'en';
        const patientContext = payload.patientContext || {};

        let systemInstruction = '';
        if (lang === 'ta') {
          systemInstruction = `நீங்கள் மெட்டிரேஸ் AI (MedTrace AI) மருத்துவ ஆலோசகர். நோயாளியின் வினாக்களுக்கு தெளிவான, துல்லியமான தமிழில் (Tamil) மட்டுமே பதிலளிக்கவும். எந்தவொரு போலி மாதிரி அல்லது டெமோ வாசகங்களையும் பயன்படுத்த வேண்டாம். இரத்தப் பரிசோதனை முடிவுகள், மருந்துகள், மருத்துவர் முன்பதிவுகள், மருத்துவக் காப்பீடு மற்றும் அருகிலுள்ள மருத்துவமனைகள் குறித்து உண்மையான வழிகாட்டலைத் தமிழில் வழங்கவும். அவசர மருத்துவ நிலைகளுக்கு 108/112 அழைக்க அறிவுறுத்தவும். நோயாளி விவரங்கள்: ` + JSON.stringify(patientContext);
        } else if (lang === 'hi') {
          systemInstruction = `आप मेडट्रेस एआई (MedTrace AI) स्वास्थ्य परामर्शदाता हैं। उपयोगकर्ता के सभी सवालों का जवाब शुद्ध, स्पष्ट और पेशेवर हिंदी (Hindi) में ही दें। कोई भी डमी या डेमो लेबल न लगाएं। स्वास्थ्य रिपोर्ट, दवाओं का समय, डॉक्टर अपॉइंटमेंट, बीमा और नजदीकी अस्पतालों के बारे में सटीक मार्गदर्शन दें। गूगल मैप्स डेटा का उपयोग करके सही अस्पतालों के पते बताएं। आपात स्थिति में 108/112 पर संपर्क करने की सलाह दें। मरीज का विवरण: ` + JSON.stringify(patientContext);
        } else {
          systemInstruction = `You are MedTrace AI Assistant, an empathetic, highly professional clinical healthcare concierge. All replies must be genuine, clinical, and completely in English without any dummy or demo placeholders. Provide clear explanations for symptoms, lab reports, medications, insurance, and hospitals using Google Maps data. Always advise users to contact emergency services (112/108) for acute symptoms. Patient context: ` + JSON.stringify(patientContext);
        }

        const contents = messages.map(m => ({
          role: m.role === 'user' ? 'user' : 'model',
          parts: [{ text: m.text }]
        }));

        const genConfig = {
          systemInstruction: systemInstruction,
          tools: [{ googleMaps: {} }]
        };

        if (userLocation && userLocation.latitude && userLocation.longitude) {
          genConfig.toolConfig = {
            retrievalConfig: {
              latLng: {
                latitude: Number(userLocation.latitude),
                longitude: Number(userLocation.longitude)
              }
            }
          };
        }

        let response = null;
        try {
          response = await ai.models.generateContent({
            model: requestedModel,
            contents: contents,
            config: genConfig
          });
        } catch (callErr) {
          console.warn('Primary model failed, attempting fallback:', callErr.message);
          response = await ai.models.generateContent({
            model: 'gemini-3.1-flash-lite',
            contents: contents,
            config: { systemInstruction }
          });
        }

        const replyText = response?.text || 'MedTrace clinical guidance is active for your healthcare queries.';
        const groundingChunks = response?.candidates?.[0]?.groundingMetadata?.groundingChunks || [];

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          reply: replyText,
          groundingChunks: groundingChunks
        }));
      } catch (err) {
        console.error('Gemini API error:', err.message);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          reply: 'MedTrace Notice: Inquiry received. Please consult emergency services or call 112/108 for urgent conditions.',
          groundingChunks: []
        }));
      }
    });
    return;
  }

  // Static File / Image Serving
  const staticMimes = {
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.png': 'image/png',
    '.webp': 'image/webp',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.css': 'text/css',
    '.js': 'application/javascript'
  };
  const ext = path.extname(url).toLowerCase();
  if (staticMimes[ext]) {
    const candidatePaths = [
      path.join(__dirname, url),
      path.join(__dirname, 'images', path.basename(url)),
      path.join(__dirname, 'app', 'src', 'main', 'res', 'drawable', path.basename(url)),
      path.join('/app/applet/images', path.basename(url)),
      path.join('/app/src/main/res/drawable', path.basename(url)),
      path.join('/app/applet/app/src/main/res/drawable', path.basename(url))
    ];
    for (const candidate of candidatePaths) {
      if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
        const stream = fs.createReadStream(candidate);
        res.writeHead(200, {
          'Content-Type': staticMimes[ext],
          'Cache-Control': 'public, max-age=86400'
        });
        stream.pipe(res);
        return;
      }
    }
  }

  // Default: Serve Web UI
  try {
    let html = fs.readFileSync(HTML_FILE, 'utf8');
    html = html.replace('__MAPS_API_KEY_PLACEHOLDER__', MAPS_API_KEY);
    res.writeHead(200, {
      'Content-Type': 'text/html; charset=utf-8',
      'Cache-Control': 'no-cache, no-store, must-revalidate'
    });
    res.end(html);
  } catch (err) {
    res.writeHead(500, { 'Content-Type': 'text/plain' });
    res.end('Error loading MedTrace UI: ' + err.message);
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('MedTrace dev server listening on http://0.0.0.0:' + PORT);
});
