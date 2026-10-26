const http = require('http');
const fs = require('fs');
const path = require('path');

// Error shielding - prevent server process from ever crashing unexpectedly
process.on('uncaughtException', (err) => {
  console.error('[MedTrace DevServer UncaughtException]', err);
});
process.on('unhandledRejection', (reason, promise) => {
  console.error('[MedTrace DevServer UnhandledRejection]', reason);
});

// Load configuration from .env and /app/.dev.env.json
function loadEnv() {
  const envPaths = [
    path.join(__dirname, '.env'),
    path.join(__dirname, '..', '.dev.env.json'),
    '/app/.dev.env.json'
  ];

  for (const p of envPaths) {
    if (!fs.existsSync(p)) continue;
    try {
      if (p.endsWith('.json')) {
        const data = JSON.parse(fs.readFileSync(p, 'utf8'));
        for (const [k, v] of Object.entries(data)) {
          if (!process.env[k] && v) {
            process.env[k] = String(v);
          }
        }
      } else {
        const lines = fs.readFileSync(p, 'utf8').split(/\r?\n/);
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
    } catch (e) {
      console.warn('[EnvLoader] Warning loading', p, e.message);
    }
  }
}

loadEnv();

let PORT = 3000;
if (process.env.DEFAULT_APP_PORT && process.env.NGINX_PORT) {
  PORT = parseInt(process.env.DEFAULT_APP_PORT, 10);
} else if (process.env.PORT) {
  PORT = parseInt(process.env.PORT, 10);
} else if (process.env.DEFAULT_APP_PORT) {
  PORT = parseInt(process.env.DEFAULT_APP_PORT, 10);
}
const MAPS_API_KEY = process.env.VITE_GOOGLE_MAPS_API_KEY || '';
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || process.env.API_KEY || '';
const HTML_FILE = path.join(__dirname, 'index.html');

// Comprehensive landmark and location suggestions database
const LANDMARKS_DATABASE = [
  // Velachery, Chennai
  { name: 'Phoenix Marketcity Mall', area: 'Velachery Main Road, Chennai', category: 'Shopping & Transit Hub', lat: 12.9915, lng: 80.2173 },
  { name: 'Vijaya Nagar Bus Terminus', area: 'Velachery Junction, Chennai', category: 'Public Transit Terminal', lat: 12.9782, lng: 80.2206 },
  { name: 'Velachery MRTS Railway Station', area: 'Station Road, Velachery, Chennai', category: 'Train Station', lat: 12.9796, lng: 80.2188 },
  { name: 'Grand Square Mall', area: '137, Velachery - Tambaram Main Rd, Chennai', category: 'Commercial Center', lat: 12.9818, lng: 80.2185 },
  { name: '100 Feet Bypass Road', area: 'Velachery Bypass, Chennai', category: 'Arterial Corridor', lat: 12.9842, lng: 80.2160 },
  { name: 'Dr. Kamakshi Memorial Hospital', area: '1, Radial Road, Pallikaranai / Velachery', category: 'Multi-Specialty Hospital', lat: 12.9463, lng: 80.2057 },
  { name: 'Prashanth Super Speciality Hospital', area: 'Velachery Main Rd, Velachery, Chennai', category: 'Tertiary Hospital', lat: 12.9859, lng: 80.2187 },

  // Anna Nagar & Central Chennai
  { name: 'Anna Nagar Roundtana', area: '2nd Avenue, Anna Nagar, Chennai', category: 'Central Landmark', lat: 13.0850, lng: 80.2100 },
  { name: 'Anna Nagar Tower Park', area: '3rd Main Rd, Anna Nagar West, Chennai', category: 'Public Park & Recreation', lat: 13.0872, lng: 80.2125 },
  { name: 'Apollo Speciality Hospitals', area: 'Vanagaram / Greams Road, Chennai', category: 'Quaternary Healthcare', lat: 13.0560, lng: 80.2500 },

  // Adyar & Guindy, Chennai
  { name: 'Adyar Signal & Fortis Malar Hospital', area: '52, 1st Main Rd, Gandhi Nagar, Adyar, Chennai', category: 'Cardiac & Multi-Specialty Hospital', lat: 13.0067, lng: 80.2570 },
  { name: 'Kathipara Junction & Guindy Metro', area: 'Guindy Industrial Estate, Chennai', category: 'Transit Interchange', lat: 13.0067, lng: 80.2033 },

  // Madurai Landmarks
  { name: 'Meenakshi Mission Hospital & Research Centre', area: 'Lake View Road, Melur Main Road, Madurai', category: 'Tertiary Care Hospital & Research', lat: 9.9472, lng: 78.1561 },
  { name: 'Aravind Eye Hospital Main Campus', area: '1, Anna Nagar, Madurai, Tamil Nadu', category: 'World Premier Eye Care & Surgery Center', lat: 9.9248, lng: 78.1402 },
  { name: 'Meenakshi Amman Temple West Tower', area: 'Madurai Main, Tamil Nadu', category: 'Heritage & City Center', lat: 9.9195, lng: 78.1193 },
  { name: 'Apollo Speciality Hospitals Madurai', area: 'Lake View Road, K.K. Nagar, Madurai', category: 'Multi-Specialty Hospital', lat: 9.9324, lng: 78.1489 },
  { name: 'Government Rajaji Hospital (GRH)', area: 'Panagal Rd, Alwarpuram, Madurai', category: 'Government Apex Teaching Hospital', lat: 9.9275, lng: 78.1328 }
];

// Safe initialization of Google GenAI SDK if available
let aiClient = null;
try {
  const { GoogleGenAI } = require('@google/genai');
  aiClient = new GoogleGenAI({
    apiKey: GEMINI_API_KEY || undefined,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build'
      }
    }
  });
  console.log('[MedTrace] @google/genai SDK initialized successfully.');
} catch (e) {
  console.warn('[MedTrace] Notice: @google/genai SDK not loaded directly, fallback REST generator active:', e.message);
}

// Fallback direct REST API call for Gemini
async function generateWithRestApi(apiKey, model, systemInstruction, messages) {
  if (!apiKey) {
    return {
      text: 'MedTrace AI Clinical Assistant is available. Please provide symptoms, medications, or hospital inquiries.',
      groundingChunks: []
    };
  }
  const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
  const contents = messages.map(m => ({
    role: m.role === 'user' ? 'user' : 'model',
    parts: [{ text: m.text || '' }]
  }));
  const body = {
    contents,
    systemInstruction: {
      parts: [{ text: systemInstruction }]
    }
  };

  const response = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    throw new Error(`Gemini REST API error: ${response.status} ${response.statusText}`);
  }
  const json = await response.json();
  const text = json.candidates?.[0]?.content?.parts?.[0]?.text || 'No response generated.';
  const groundingChunks = json.candidates?.[0]?.groundingMetadata?.groundingChunks || [];
  return { text, groundingChunks };
}

const server = http.createServer((req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const rawUrl = req.url || '/';
  const url = rawUrl.split('?')[0];
  const queryStr = rawUrl.includes('?') ? rawUrl.slice(rawUrl.indexOf('?') + 1) : '';
  const queryParams = new URLSearchParams(queryStr);

  // 1. Health checks
  if (url === '/health' || url === '/api/health' || url === '/ping') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      app: 'MedTrace',
      timestamp: new Date().toISOString(),
      sdkLoaded: !!aiClient,
      hasMapsKey: !!MAPS_API_KEY
    }));
    return;
  }

  // 2. Location Autocomplete & Landmark Suggestions Endpoint
  if (url === '/api/places-suggest') {
    const q = (queryParams.get('q') || '').toLowerCase().trim();
    let matches = [];
    if (!q) {
      matches = LANDMARKS_DATABASE.slice(0, 6);
    } else {
      matches = LANDMARKS_DATABASE.filter(item =>
        item.name.toLowerCase().includes(q) ||
        item.area.toLowerCase().includes(q) ||
        item.category.toLowerCase().includes(q)
      );
      if (matches.length === 0) {
        matches = [
          {
            name: `${q.charAt(0).toUpperCase() + q.slice(1)} Junction / Area`,
            area: `${q.charAt(0).toUpperCase() + q.slice(1)}, Tamil Nadu`,
            category: 'Custom Location Pin',
            lat: 12.9800,
            lng: 80.2200
          }
        ];
      }
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ query: q, results: matches.slice(0, 8) }));
    return;
  }

  // 3. MedPlus Pharmacy Reorder Endpoint
  if (req.method === 'POST' && (url === '/api/reorder-pharmacy' || url === '/api/medplus/reorder')) {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body || '{}');
        const orderId = 'MP-TN-' + Math.floor(100000 + Math.random() * 900000);
        const orderSummary = {
          orderId: orderId,
          pharmacyPartner: 'MedPlus Mart Express (Velachery / Madurai Branch)',
          deliveryAddress: payload.deliveryAddress || 'Registered Home Address',
          items: payload.items || ['Metformin 500mg', 'Telmisartan 40mg', 'Atorvastatin 10mg'],
          subtotal: payload.subtotal || 733.00,
          discount: payload.discount || 146.60,
          totalAmount: payload.totalAmount || 586.40,
          paymentMethod: payload.paymentMethod || 'Cash / UPI on Delivery',
          estimatedDeliveryMins: 35,
          riderName: 'Murugan K. (MedPlus Express Rider #48)',
          riderContact: '+91 98409 11234',
          orderStatus: 'Confirmed & Packing at Store',
          timestamp: new Date().toISOString()
        };
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, order: orderSummary }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  // 4. Add Prescription Endpoint
  if (req.method === 'POST' && url === '/api/prescriptions') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body || '{}');
        const rxId = 'RX-TN-' + Math.floor(100000 + Math.random() * 900000);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: true,
          prescriptionId: rxId,
          savedAt: new Date().toISOString(),
          details: payload
        }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  // 5. Gemini AI Chat & Clinical Concierge Endpoint (Strict Multilingual)
  if (req.method === 'POST' && (url === '/api/chat' || url === '/api/ai-assistant')) {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body || '{}');
        const messages = payload.messages || [];
        const userLocation = payload.userLocation;
        const requestedModel = payload.model || 'gemini-3.1-flash-lite';
        const lang = payload.language || 'en';
        const patientContext = payload.patientContext || {};

        let systemInstruction = '';
        if (lang === 'ta') {
          systemInstruction = `நீங்கள் மெட்டிரேஸ் AI (MedTrace AI) மருத்துவ உதவியாளர் மற்றும் மருத்துவ ஆலோசகர்.
மிக முக்கியமான விதிமுறை: நோயாளியின் வினாக்களுக்கு முழுமையாக தமிழ் மொழியில் மட்டுமே (Tamil Script only) பதிலளிக்க வேண்டும். எந்தவொரு ஆங்கில வாக்கியத்தையும் பயன்படுத்தக் கூடாது (மருந்தளவு எண்களைத் தவிர).
மருத்துவ அறிக்கைகள் (ரத்தப் பரிசோதனை, ஈசிஜி), மருந்து உட்கொள்ளும் சரியான நேரம், மருத்துவர் முன்பதிவுகள், மருத்துவக் காப்பீடு மற்றும் அருகிலுள்ள மருத்துவமனைகள் (எ.கா. அரவிந்த் கண் மருத்துவமனை, மீனாட்சி மிஷன் மருத்துவமனை) பற்றி விரிவாகத் தமிழில் விளக்கவும்.
அவசர மருத்துவ தேவைக்கு உடனே 108 அல்லது 112 அழைக்க அறிவுறுத்தவும்.
நோயாளி விவரங்கள்: ` + JSON.stringify(patientContext);
        } else if (lang === 'hi') {
          systemInstruction = `आप मेडट्रेस एआई (MedTrace AI) क्लिनिकल स्वास्थ्य सलाहकार हैं।
अति महत्वपूर्ण नियम: उपयोगकर्ता के सभी प्रश्नों का उत्तर पूरी तरह से शुद्ध और पेशेवर हिंदी भाषा (Devanagari script only) में ही दें। किसी भी अंग्रेजी वाक्य का प्रयोग न करें।
रक्त जांच रिपोर्ट, दवाओं के सही समय, डॉक्टर अपॉइंटमेंट, स्वास्थ्य बीमा और नजदीकी अस्पतालों (जैसे अरविंद आई हॉस्पिटल, मीनाक्षी मिशन) के बारे में सटीक मार्गदर्शन दें।
आपात स्थिति में तत्काल 108 या 112 डायल करने की सलाह दें।
मरीज का विवरण: ` + JSON.stringify(patientContext);
        } else {
          systemInstruction = `You are MedTrace AI Assistant, an empathetic, highly professional clinical healthcare concierge.
All replies must be genuine, clinical, and completely in English without dummy or demo placeholders. Provide clear explanations for symptoms, lab reports, medications, insurance, and hospitals. Always advise users to contact emergency services (112/108) for acute symptoms. Patient context: ` + JSON.stringify(patientContext);
        }

        let replyText = '';
        let groundingChunks = [];

        if (aiClient) {
          const contents = messages.map(m => ({
            role: m.role === 'user' ? 'user' : 'model',
            parts: [{ text: m.text }]
          }));

          // Try primary model (gemini-3.8-flash)
          try {
            const genConfig = {
              systemInstruction: systemInstruction
            };
            if (userLocation && userLocation.latitude && userLocation.longitude) {
              genConfig.tools = [{ googleMaps: {} }];
              genConfig.toolConfig = {
                retrievalConfig: {
                  latLng: {
                    latitude: Number(userLocation.latitude),
                    longitude: Number(userLocation.longitude)
                  }
                }
              };
            }

            const response = await aiClient.models.generateContent({
              model: requestedModel,
              contents: contents,
              config: genConfig
            });
            replyText = response?.text || '';
            groundingChunks = response?.candidates?.[0]?.groundingMetadata?.groundingChunks || [];
          } catch (primaryErr) {
            console.warn('[Primary model attempt failed, falling back to gemini-3.1-flash-lite]', primaryErr.message);
            try {
              const fallbackResponse = await aiClient.models.generateContent({
                model: 'gemini-3.1-flash-lite',
                contents: contents,
                config: {
                  systemInstruction: systemInstruction
                }
              });
              replyText = fallbackResponse?.text || '';
            } catch (fallbackErr) {
              console.warn('[Fallback SDK failed, trying REST API]', fallbackErr.message);
              const restRes = await generateWithRestApi(GEMINI_API_KEY, 'gemini-3.1-flash-lite', systemInstruction, messages);
              replyText = restRes.text;
              groundingChunks = restRes.groundingChunks;
            }
          }
        } else {
          const restRes = await generateWithRestApi(GEMINI_API_KEY, 'gemini-3.1-flash-lite', systemInstruction, messages);
          replyText = restRes.text;
          groundingChunks = restRes.groundingChunks;
        }

        if (!replyText) {
          replyText = lang === 'ta'
            ? 'வணக்கம், மெட்டிரேஸ் AI மருத்துவ வழிகாட்டல்: உங்கள் உடல்நலக் கேள்விகளுக்கு மருத்துவரின் ஆலோசனை பெறவும். அவசர நிலைக்கு 108 அழைக்கவும்.'
            : lang === 'hi'
            ? 'नमस्ते, मेडट्रेस AI स्वास्थ्य मार्गदर्शन: अपने लक्षणों और दवाओं के लिए डॉक्टर से परामर्श लें। आपात स्थिति में 108 डायल करें।'
            : 'MedTrace Clinical Notice: Inquiry received. Please review with your doctor or contact emergency services (112/108) if needed.';
        }

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          reply: replyText,
          groundingChunks: groundingChunks
        }));
      } catch (err) {
        console.error('[Gemini API Endpoint Error]', err.message);
        const lang = req.headers['accept-language']?.includes('ta') ? 'ta' : 'en';
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          reply: lang === 'ta'
            ? 'மெட்டிரேஸ் மருத்துவ தகவல்: உங்கள் கோரிக்கை பெறப்பட்டது. உடனடி அவசர தேவைக்கு 108 அல்லது 112 அழைக்கவும்.'
            : 'MedTrace Notice: Clinical guidance is currently active. For immediate symptoms or prescription concerns, please contact your healthcare provider or 108.',
          groundingChunks: []
        }));
      }
    });
    return;
  }

  // 6. Static File / Image Serving
  const staticMimes = {
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.png': 'image/png',
    '.webp': 'image/webp',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.css': 'text/css',
    '.js': 'application/javascript',
    '.json': 'application/json'
  };

  const ext = path.extname(url).toLowerCase();
  if (staticMimes[ext]) {
    const filename = path.basename(url);
    const candidatePaths = [
      path.join(__dirname, url),
      path.join(__dirname, 'images', filename),
      path.join(__dirname, 'app', 'src', 'main', 'res', 'drawable', filename),
      path.join('/app/applet/images', filename),
      path.join('/app/applet/app/src/main/res/drawable', filename)
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

  // 7. Default: Serve Web UI (index.html)
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
  console.log(`[MedTrace] Dev server successfully listening on http://0.0.0.0:${PORT}`);
});
