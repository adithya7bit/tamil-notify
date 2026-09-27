import React, { useState, useEffect, useRef } from 'react';
import { 
  Bell, 
  Send, 
  Copy, 
  Share2, 
  Volume2, 
  Mic, 
  MicOff, 
  Sparkles, 
  Settings as SettingsIcon, 
  ListFilter, 
  ShieldCheck, 
  Download, 
  Check, 
  ExternalLink,
  MessageSquare,
  Smartphone,
  Trash2,
  Globe
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { supabase } from './supabaseClient';
import { protectTokens, translateNotificationText } from './tokenProtector';

const PRESET_MESSAGES = [
  {
    id: 'whatsapp',
    label: '💬 WhatsApp',
    title: 'Priya (Accounts)',
    text: 'Please send $450 to Rahul by 5:00 PM today. OTP verification code is 849201. Review receipt at https://pay.me/12345'
  },
  {
    id: 'bank',
    label: '🏦 Bank OTP',
    title: 'HDFC Bank Alert',
    text: 'Rs. 4,500.00 debited from A/C 9876543210 on 27/09/2026. OTP for approval is 392810. Do not share.'
  },
  {
    id: 'food',
    label: '🛵 Food Delivery',
    title: 'Swiggy Delivery',
    text: 'Your order is arriving in 12 mins. OTP is 7419. Call delivery partner at +91 9876543210.'
  },
  {
    id: 'flight',
    label: '✈️ Flight Alert',
    title: 'Indigo Airlines',
    text: 'Flight 6E-204 to Chennai boards at Gate 4B at 17:30. Check status at https://goindigo.in/status'
  }
];

export default function App() {
  const [activeTab, setActiveTab] = useState('translate'); // 'translate', 'feed', 'settings'
  const [inputText, setInputText] = useState(PRESET_MESSAGES[0].text);
  const [senderTitle, setSenderTitle] = useState(PRESET_MESSAGES[0].title);
  const [translatedText, setTranslatedText] = useState('');
  const [isTranslating, setIsTranslating] = useState(false);
  const [copied, setCopied] = useState(false);
  const [targetLang, setTargetLang] = useState('ta'); // Tamil
  const [history, setHistory] = useState([]);
  const [isListening, setIsListening] = useState(false);
  const [protectedTokenCount, setProtectedTokenCount] = useState(0);
  const [notificationPermission, setNotificationPermission] = useState('default');
  const [deferredPrompt, setDeferredPrompt] = useState(null);
  const [displayMode, setDisplayMode] = useState('both'); // 'both', 'tamil_only'

  // Register service worker and check notification permission
  useEffect(() => {
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js').catch(err => {
        console.warn('SW registration failed:', err);
      });
    }

    if ('Notification' in window) {
      setNotificationPermission(Notification.permission);
    }

    // Capture PWA install prompt
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      setDeferredPrompt(e);
    });

    // Load initial history from localStorage
    const saved = localStorage.getItem('tamil_notify_history');
    if (saved) {
      try {
        setHistory(JSON.parse(saved));
      } catch (_) {}
    }

    // Run initial demo translation
    handleTranslate(PRESET_MESSAGES[0].text, PRESET_MESSAGES[0].title);
  }, []);

  // Update token count preview
  useEffect(() => {
    const { tokenMap } = protectTokens(inputText);
    setProtectedTokenCount(Object.keys(tokenMap).length);
  }, [inputText]);

  // Request Web Notification permission
  const requestNotificationPermission = async () => {
    if (!('Notification' in window)) {
      alert('This browser does not support desktop notifications.');
      return;
    }
    const permission = await Notification.requestPermission();
    setNotificationPermission(permission);
    if (permission === 'granted') {
      confetti({ particleCount: 50, spread: 60, origin: { y: 0.8 } });
    }
  };

  // Perform translation
  const handleTranslate = async (textToUse = inputText, titleToUse = senderTitle) => {
    if (!textToUse.trim()) return;

    setIsTranslating(true);
    try {
      const translated = await translateNotificationText(textToUse, targetLang);
      setTranslatedText(translated);

      const newItem = {
        id: Date.now(),
        title: titleToUse || 'Custom Notification',
        original: textToUse,
        translated: translated,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        date: new Date().toISOString()
      };

      setHistory(prev => {
        const updated = [newItem, ...prev.slice(0, 49)];
        localStorage.setItem('tamil_notify_history', JSON.stringify(updated));
        return updated;
      });

      // Optional sync with Supabase if table exists
      try {
        supabase.from('notification_translations').insert([{
          title: newItem.title,
          original_text: newItem.original,
          translated_text: newItem.translated,
          target_lang: targetLang
        }]).then(() => {});
      } catch (_) {}

    } catch (err) {
      console.error('Translation failed:', err);
    } finally {
      setIsTranslating(false);
    }
  };

  // Trigger Native Web Notification on Mobile/Desktop
  const triggerNativeNotification = () => {
    if (!('Notification' in window)) {
      alert('Web notifications not supported on this browser.');
      return;
    }

    if (Notification.permission !== 'granted') {
      Notification.requestPermission().then(permission => {
        setNotificationPermission(permission);
        if (permission === 'granted') {
          showNotification();
        }
      });
    } else {
      showNotification();
    }
  };

  const showNotification = () => {
    const title = `${senderTitle || 'Tamil Notify'} • தமிழ்`;
    const body = displayMode === 'both' 
      ? `${translatedText}\n\nOriginal: ${inputText}`
      : translatedText;

    if (navigator.serviceWorker && navigator.serviceWorker.controller) {
      navigator.serviceWorker.ready.then(reg => {
        reg.showNotification(title, {
          body,
          icon: '/icon.svg',
          badge: '/icon.svg',
          vibrate: [200, 100, 200],
          tag: 'tamil-notify-' + Date.now()
        });
      });
    } else {
      new Notification(title, {
        body,
        icon: '/icon.svg'
      });
    }

    confetti({ particleCount: 70, spread: 70, origin: { y: 0.7 } });
  };

  // Text to Speech
  const speakTamil = (text) => {
    if (!('speechSynthesis' in window)) {
      alert('Speech synthesis not supported in this browser.');
      return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'ta-IN';
    utterance.rate = 0.9;
    window.speechSynthesis.speak(utterance);
  };

  // Speech to text toggle
  const toggleSpeechRecognition = () => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert('Speech recognition is not supported in this browser.');
      return;
    }

    if (isListening) {
      setIsListening(false);
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.lang = 'en-US';
    recognition.interimResults = false;
    recognition.onstart = () => setIsListening(true);
    recognition.onend = () => setIsListening(false);
    recognition.onerror = () => setIsListening(false);
    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      setInputText(transcript);
      handleTranslate(transcript, 'Voice Input');
    };
    recognition.start();
  };

  // Copy to clipboard
  const handleCopy = () => {
    navigator.clipboard.writeText(translatedText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Share via Web Share API
  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: senderTitle,
          text: `${translatedText}\n\nOriginal: ${inputText}`,
          url: window.location.href
        });
      } catch (_) {}
    } else {
      handleCopy();
    }
  };

  // PWA Install
  const installPWA = async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    if (outcome === 'accepted') {
      setDeferredPrompt(null);
    }
  };

  return (
    <>
      {/* Header */}
      <header className="app-header">
        <div className="brand-wrapper">
          <div className="brand-icon">
            <Bell size={22} color="#0F172A" />
          </div>
          <div>
            <h1 className="brand-title">Tamil Notify</h1>
            <p className="brand-subtitle">அறிவிப்பு மொழிபெயர்ப்பாளர்</p>
          </div>
        </div>

        <div className="status-badge">
          <span className="status-dot"></span>
          <span>Online</span>
        </div>
      </header>

      {/* Main Container */}
      <main className="app-content">
        {/* PWA Install Banner */}
        {deferredPrompt && (
          <div className="install-banner">
            <div>
              <p style={{ fontSize: '0.82rem', fontWeight: 'bold' }}>Install Tamil Notify PWA</p>
              <p style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Add to mobile home screen for quick access</p>
            </div>
            <button className="install-btn" onClick={installPWA}>Install</button>
          </div>
        )}

        {/* TAB 1: TRANSLATOR & NOTIFICATION SIMULATOR */}
        {activeTab === 'translate' && (
          <>
            <div className="card">
              <h2 className="card-title">
                <Sparkles size={18} color="var(--primary)" />
                Notification Simulator & Translator
              </h2>
              <p className="card-desc">
                Select a real-world scenario or type any message. URLs, OTPs, currency amounts, and phone numbers are protected verbatim.
              </p>

              {/* Preset Scenario Chips */}
              <div className="preset-scroll">
                {PRESET_MESSAGES.map(preset => (
                  <button
                    key={preset.id}
                    className={`chip-btn ${inputText === preset.text ? 'active' : ''}`}
                    onClick={() => {
                      setInputText(preset.text);
                      setSenderTitle(preset.title);
                      handleTranslate(preset.text, preset.title);
                    }}
                  >
                    {preset.label}
                  </button>
                ))}
              </div>

              {/* Input Area */}
              <div className="input-box-wrapper">
                <textarea
                  className="input-box"
                  value={inputText}
                  onChange={(e) => setInputText(e.target.value)}
                  placeholder="Enter message text with OTPs, currency, or URLs..."
                  rows={4}
                />

                <div className="input-actions">
                  <div className="token-indicator">
                    <ShieldCheck size={14} />
                    <span>{protectedTokenCount} Sensitive Tokens Protected</span>
                  </div>

                  <div className="tool-buttons">
                    <button
                      className={`icon-btn ${isListening ? 'active' : ''}`}
                      onClick={toggleSpeechRecognition}
                      title="Voice Speech to Text"
                    >
                      {isListening ? <MicOff size={16} /> : <Mic size={16} />}
                    </button>
                    {inputText && (
                      <button
                        className="icon-btn"
                        onClick={() => setInputText('')}
                        title="Clear Text"
                      >
                        <Trash2 size={15} />
                      </button>
                    )}
                  </div>
                </div>
              </div>

              <div style={{ marginTop: '12px' }}>
                <button
                  className="btn-primary"
                  onClick={() => handleTranslate()}
                  disabled={isTranslating || !inputText.trim()}
                >
                  <Send size={18} />
                  <span>{isTranslating ? 'Translating on Device...' : 'Translate to Tamil'}</span>
                </button>
              </div>
            </div>

            {/* Translated Output Card */}
            {translatedText && (
              <div className="output-card">
                <div className="output-header">
                  <div className="tamil-badge">
                    <Globe size={15} />
                    <span>தமிழ் மொழிபெயர்ப்பு</span>
                  </div>
                  <button
                    className="icon-btn"
                    onClick={() => speakTamil(translatedText)}
                    title="Pronounce in Tamil"
                  >
                    <Volume2 size={16} />
                  </button>
                </div>

                <div className="translated-text">
                  {translatedText}
                </div>

                {displayMode === 'both' && (
                  <div className="original-peek">
                    <span style={{ fontWeight: '600', color: 'var(--text-main)' }}>Original: </span>
                    {inputText}
                  </div>
                )}

                <div className="action-row">
                  <button className="action-btn" onClick={handleCopy}>
                    {copied ? <Check size={14} color="var(--emerald)" /> : <Copy size={14} />}
                    <span>{copied ? 'Copied!' : 'Copy'}</span>
                  </button>

                  <button className="action-btn" onClick={handleShare}>
                    <Share2 size={14} />
                    <span>Share</span>
                  </button>

                  <button className="action-btn notify-btn" onClick={triggerNativeNotification}>
                    <Bell size={14} />
                    <span>Send to Status Bar</span>
                  </button>
                </div>
              </div>
            )}
          </>
        )}

        {/* TAB 2: LIVE FEED & CLOUD HISTORY */}
        {activeTab === 'feed' && (
          <div className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <h2 className="card-title">
                <ListFilter size={18} color="var(--primary)" />
                Notification Stream History
              </h2>
              {history.length > 0 && (
                <button 
                  className="chip-btn" 
                  onClick={() => {
                    setHistory([]);
                    localStorage.removeItem('tamil_notify_history');
                  }}
                >
                  Clear History
                </button>
              )}
            </div>

            {history.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '36px 0', color: 'var(--text-muted)' }}>
                <Bell size={40} style={{ opacity: 0.3, marginBottom: '12px' }} />
                <p style={{ fontWeight: '600', fontSize: '0.95rem' }}>No captured notifications yet</p>
                <p style={{ fontSize: '0.78rem', marginTop: '4px' }}>Simulate or translate messages to see your history feed here.</p>
              </div>
            ) : (
              <div>
                {history.map(item => (
                  <div key={item.id} className="feed-item">
                    <div className="feed-header">
                      <span className="app-pill">
                        <MessageSquare size={13} color="var(--primary)" />
                        {item.title}
                      </span>
                      <span className="feed-time">{item.time}</span>
                    </div>
                    <div className="feed-content" style={{ fontFamily: "'Noto Sans Tamil', sans-serif" }}>
                      {item.translated}
                    </div>
                    <div className="feed-original">
                      Original: {item.original}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: SETTINGS & PERMISSIONS */}
        {activeTab === 'settings' && (
          <div className="card">
            <h2 className="card-title">
              <SettingsIcon size={18} color="var(--primary)" />
              App & Mobile Permissions
            </h2>
            <p className="card-desc">
              Configure Web Notifications, language settings, and cloud synchronization with Supabase.
            </p>

            {/* Notification Permission Card */}
            <div style={{ background: 'var(--bg-input)', padding: '14px', borderRadius: 'var(--radius-sm)', marginBottom: '14px', border: '1px solid var(--border-color)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <p style={{ fontSize: '0.88rem', fontWeight: 'bold' }}>Native Device Notifications</p>
                  <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    Status: <span style={{ color: notificationPermission === 'granted' ? 'var(--emerald)' : 'var(--amber)', fontWeight: 'bold' }}>{notificationPermission.toUpperCase()}</span>
                  </p>
                </div>
                {notificationPermission !== 'granted' && (
                  <button className="chip-btn active" onClick={requestNotificationPermission}>
                    Enable Alerts
                  </button>
                )}
              </div>
            </div>

            {/* Display Mode Selection */}
            <div style={{ marginBottom: '16px' }}>
              <p style={{ fontSize: '0.85rem', fontWeight: 'bold', marginBottom: '8px' }}>Notification Display Mode</p>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  className={`chip-btn ${displayMode === 'both' ? 'active' : ''}`}
                  onClick={() => setDisplayMode('both')}
                  style={{ flex: 1 }}
                >
                  Tamil + Original
                </button>
                <button
                  className={`chip-btn ${displayMode === 'tamil_only' ? 'active' : ''}`}
                  onClick={() => setDisplayMode('tamil_only')}
                  style={{ flex: 1 }}
                >
                  Tamil Only
                </button>
              </div>
            </div>

            {/* Supabase Integration Note */}
            <div style={{ background: 'rgba(249, 115, 22, 0.08)', padding: '14px', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(249, 115, 22, 0.25)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                <ShieldCheck size={18} color="var(--primary)" />
                <span style={{ fontSize: '0.85rem', fontWeight: 'bold', color: 'var(--primary)' }}>Supabase Cloud Connection</span>
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', lineHeight: '1.4' }}>
                Connected to project <code>tjgyjtlmzotcapeqndpx</code> for real-time history and cross-device sync.
              </p>
            </div>
          </div>
        )}
      </main>

      {/* Bottom Navigation */}
      <nav className="bottom-nav">
        <button
          className={`nav-item ${activeTab === 'translate' ? 'active' : ''}`}
          onClick={() => setActiveTab('translate')}
        >
          <Sparkles size={20} />
          <span>Translate</span>
        </button>

        <button
          className={`nav-item ${activeTab === 'feed' ? 'active' : ''}`}
          onClick={() => setActiveTab('feed')}
        >
          <Bell size={20} />
          <span>History</span>
        </button>

        <button
          className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveTab('settings')}
        >
          <SettingsIcon size={20} />
          <span>Settings</span>
        </button>
      </nav>
    </>
  );
}
