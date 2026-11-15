'use client';

import React, { useState, useEffect, useRef } from 'react';
import QRCode from 'qrcode';
import { Navbar } from '@/components/Navbar';
import { Footer } from '@/components/Footer';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import {
  QrCode,
  Download,
  Link as LinkIcon,
  Type,
  Wifi,
  Phone,
  MessageSquare,
  CheckCircle2,
  Copy,
  Palette,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

const COUNTRY_CODES = [
  { code: '+91', country: 'India', flag: '🇮🇳' },
  { code: '+1', country: 'USA / Canada', flag: '🇺🇸' },
  { code: '+44', country: 'United Kingdom', flag: '🇬🇧' },
  { code: '+971', country: 'UAE', flag: '🇦🇪' },
  { code: '+966', country: 'Saudi Arabia', flag: '🇸🇦' },
  { code: '+61', country: 'Australia', flag: '🇦🇺' },
  { code: '+49', country: 'Germany', flag: '🇩🇪' },
  { code: '+33', country: 'France', flag: '🇫🇷' },
  { code: '+65', country: 'Singapore', flag: '🇸🇬' },
  { code: '+60', country: 'Malaysia', flag: '🇲🇾' },
  { code: '+880', country: 'Bangladesh', flag: '🇧🇩' },
  { code: '+977', country: 'Nepal', flag: '🇳🇵' },
  { code: '+92', country: 'Pakistan', flag: '🇵🇰' },
  { code: '+94', country: 'Sri Lanka', flag: '🇱🇰' },
  { code: '+62', country: 'Indonesia', flag: '🇮🇩' },
  { code: '+81', country: 'Japan', flag: '🇯🇵' },
  { code: '+82', country: 'South Korea', flag: '🇰🇷' },
  { code: '+55', country: 'Brazil', flag: '🇧🇷' },
  { code: '+7', country: 'Russia', flag: '🇷🇺' },
  { code: '+39', country: 'Italy', flag: '🇮🇹' },
  { code: '+34', country: 'Spain', flag: '🇪🇸' },
  { code: '+31', country: 'Netherlands', flag: '🇳🇱' },
  { code: '+41', country: 'Switzerland', flag: '🇨🇭' },
  { code: '+27', country: 'South Africa', flag: '🇿🇦' },
  { code: '+234', country: 'Nigeria', flag: '🇳🇬' },
];

export default function QrCodePage() {
  const [qrType, setQrType] = useState<'url' | 'text' | 'wifi' | 'whatsapp' | 'phone'>('url');
  const [urlValue, setUrlValue] = useState('https://ai-chat2pdf.live');
  const [textValue, setTextValue] = useState('');
  const [wifiSsid, setWifiSsid] = useState('');
  const [wifiPassword, setWifiPassword] = useState('');
  const [wifiEncryption, setWifiEncryption] = useState<'WPA' | 'WEP' | 'nopass'>('WPA');
  const [selectedCountryCode, setSelectedCountryCode] = useState('+91');
  const [waPhone, setWaPhone] = useState('9876543210');
  const [waMessage, setWaMessage] = useState('Hello from Qwik Save QR Code!');
  const [phoneNumber, setPhoneNumber] = useState('9876543210');

  const [fgColor, setFgColor] = useState('#000000');
  const [bgColor, setBgColor] = useState('#ffffff');
  const [errorLevel, setErrorLevel] = useState<'L' | 'M' | 'Q' | 'H'>('M');
  const [qrDataUrl, setQrDataUrl] = useState('');
  const [copied, setCopied] = useState(false);
  const [faqOpen, setFaqOpen] = useState<number | null>(null);

  const getRawContent = () => {
    switch (qrType) {
      case 'url':
        return urlValue.trim() || 'https://ai-chat2pdf.live';
      case 'text':
        return textValue.trim() || 'Hello World!';
      case 'wifi':
        return `WIFI:S:${wifiSsid};T:${wifiEncryption};P:${wifiPassword};;`;
      case 'whatsapp': {
        const rawNum = waPhone.replace(/[^0-9]/g, '');
        const cleanCode = selectedCountryCode.replace(/[^0-9]/g, '');
        const fullNumber = rawNum.startsWith(cleanCode) ? rawNum : `${cleanCode}${rawNum}`;
        return `https://wa.me/${fullNumber}${
          waMessage ? `?text=${encodeURIComponent(waMessage)}` : ''
        }`;
      }
      case 'phone': {
        const rawNum = phoneNumber.replace(/[^0-9]/g, '');
        const cleanCode = selectedCountryCode.replace(/[^0-9]/g, '');
        const fullNumber = rawNum.startsWith(cleanCode) ? `+${rawNum}` : `+${cleanCode}${rawNum}`;
        return `tel:${fullNumber}`;
      }
      default:
        return urlValue || 'https://ai-chat2pdf.live';
    }
  };

  useEffect(() => {
    const rawContent = getRawContent();
    if (!rawContent) return;

    QRCode.toDataURL(
      rawContent,
      {
        width: 400,
        margin: 2,
        color: { dark: fgColor, light: bgColor },
        errorCorrectionLevel: errorLevel,
      },
      (err, url) => {
        if (!err && url) {
          setQrDataUrl(url);
        }
      }
    );
  }, [
    qrType,
    urlValue,
    textValue,
    wifiSsid,
    wifiPassword,
    wifiEncryption,
    selectedCountryCode,
    waPhone,
    waMessage,
    phoneNumber,
    fgColor,
    bgColor,
    errorLevel,
  ]);

  const handleDownloadPng = () => {
    if (!qrDataUrl) return;
    const a = document.createElement('a');
    a.href = qrDataUrl;
    a.download = `qrcode_${qrType}_${Date.now()}.png`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(getRawContent());
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const faqs = [
    {
      q: 'Do generated QR codes expire?',
      a: 'No! These are standard static QR codes that contain your exact encoded link or data directly. They will work indefinitely with any standard smartphone camera scanner.',
    },
    {
      q: 'Can people scan the Wi-Fi QR code without typing the password?',
      a: 'Yes! Scanning the Wi-Fi QR code on iOS or Android prompts the user to "Join Wi-Fi Network" automatically without manually entering the password.',
    },
  ];

  return (
    <div className="flex-1 flex flex-col min-h-screen bg-slate-50 dark:bg-[#090D14] text-slate-900 dark:text-[#E8EDF5] transition-colors duration-200">
      <Navbar />

      <main className="flex-1">
        <section className="pt-8 pb-12 sm:pt-12 sm:pb-14 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto border-b border-slate-200 dark:border-[#263448]">
          <div className="text-center space-y-3 mb-8">
            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-[2px] text-xs font-semibold bg-purple-500/10 text-purple-600 dark:text-purple-400 border border-purple-500/20">
              <QrCode className="w-3.5 h-3.5" />
              <span>QR Code Studio</span>
            </div>

            <h1 className="text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-slate-900 dark:text-[#E8EDF5]">
              Generate Custom QR Codes in HD
            </h1>

            <p className="max-w-3xl mx-auto text-xs sm:text-sm text-slate-600 dark:text-[#94A3B8] leading-relaxed">
              Create clean, high-resolution QR codes for websites, Wi-Fi credentials, WhatsApp, contact numbers, and text.
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start max-w-5xl mx-auto">
            {/* Left Column: Form & Customization */}
            <div className="lg:col-span-7 bg-white dark:bg-[#141D2B] rounded-[2px] p-5 border border-slate-200 dark:border-[#263448] shadow-sm">
              <div className="flex items-center gap-1.5 overflow-x-auto pb-2 mb-4 border-b border-slate-200 dark:border-[#263448]">
                {[
                  { id: 'url', label: 'Website URL', icon: LinkIcon },
                  { id: 'text', label: 'Plain Text', icon: Type },
                  { id: 'wifi', label: 'Wi-Fi Network', icon: Wifi },
                  { id: 'whatsapp', label: 'WhatsApp', icon: MessageSquare },
                  { id: 'phone', label: 'Phone', icon: Phone },
                ].map((tab) => {
                  const Icon = tab.icon;
                  const isSelected = qrType === tab.id;
                  return (
                    <button
                      key={tab.id}
                      onClick={() => setQrType(tab.id as any)}
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-[2px] text-xs font-medium transition ${
                        isSelected
                          ? 'bg-blue-50 dark:bg-[#192536] text-blue-600 dark:text-[#3B82F6] border border-blue-500/30 font-semibold'
                          : 'bg-slate-50 dark:bg-[#111A29] text-slate-600 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] border border-slate-200 dark:border-[#263448]'
                      }`}
                    >
                      <Icon className="w-3.5 h-3.5" />
                      <span>{tab.label}</span>
                    </button>
                  );
                })}
              </div>

              {/* Dynamic Inputs */}
              <div className="space-y-3 mb-4">
                {qrType === 'url' && (
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                      Website URL
                    </label>
                    <input
                      type="url"
                      value={urlValue}
                      onChange={(e) => setUrlValue(e.target.value)}
                      placeholder="https://example.com"
                      className="w-full h-9 px-3 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6]"
                    />
                  </div>
                )}

                {qrType === 'text' && (
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                      Text Content
                    </label>
                    <textarea
                      rows={3}
                      value={textValue}
                      onChange={(e) => setTextValue(e.target.value)}
                      placeholder="Enter notes, greetings, or text..."
                      className="w-full p-2.5 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-[#3B82F6] resize-none"
                    />
                  </div>
                )}

                {qrType === 'wifi' && (
                  <div className="space-y-2.5">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                        Network Name (SSID)
                      </label>
                      <input
                        type="text"
                        value={wifiSsid}
                        onChange={(e) => setWifiSsid(e.target.value)}
                        placeholder="MyHomeWiFi"
                        className="w-full h-9 px-3 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5]"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                        Password
                      </label>
                      <input
                        type="text"
                        value={wifiPassword}
                        onChange={(e) => setWifiPassword(e.target.value)}
                        placeholder="SecretPassword"
                        className="w-full h-9 px-3 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5]"
                      />
                    </div>
                  </div>
                )}

                {qrType === 'whatsapp' && (
                  <div className="space-y-3">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                        Country Code & WhatsApp Number
                      </label>
                      <div className="flex items-center gap-2">
                        <select
                          value={selectedCountryCode}
                          onChange={(e) => setSelectedCountryCode(e.target.value)}
                          className="h-9 px-2.5 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-blue-500 shrink-0 font-medium"
                        >
                          {COUNTRY_CODES.map((c) => (
                            <option key={c.code} value={c.code}>
                              {c.flag} {c.code} ({c.country})
                            </option>
                          ))}
                        </select>
                        <input
                          type="tel"
                          value={waPhone}
                          onChange={(e) => setWaPhone(e.target.value)}
                          placeholder="9876543210"
                          className="flex-1 h-9 px-3 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-blue-500 font-mono"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                        Pre-filled WhatsApp Message (Optional)
                      </label>
                      <textarea
                        rows={2}
                        value={waMessage}
                        onChange={(e) => setWaMessage(e.target.value)}
                        placeholder="Hi! I got your contact from Qwik Save QR code..."
                        className="w-full p-2.5 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-blue-500 resize-none"
                      />
                    </div>
                  </div>
                )}

                {qrType === 'phone' && (
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-[#94A3B8] mb-1">
                      Country Code & Phone Number
                    </label>
                    <div className="flex items-center gap-2">
                      <select
                        value={selectedCountryCode}
                        onChange={(e) => setSelectedCountryCode(e.target.value)}
                        className="h-9 px-2.5 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-blue-500 shrink-0 font-medium"
                      >
                        {COUNTRY_CODES.map((c) => (
                          <option key={c.code} value={c.code}>
                            {c.flag} {c.code} ({c.country})
                          </option>
                        ))}
                      </select>
                      <input
                        type="tel"
                        value={phoneNumber}
                        onChange={(e) => setPhoneNumber(e.target.value)}
                        placeholder="9876543210"
                        className="flex-1 h-9 px-3 rounded-[2px] bg-slate-50 dark:bg-[#0F1623] border border-slate-300 dark:border-[#263448] text-xs text-slate-900 dark:text-[#E8EDF5] focus:outline-none focus:border-blue-500 font-mono"
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Color Customization */}
              <div className="pt-3 border-t border-slate-200 dark:border-[#263448]">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[11px] font-medium text-slate-500 dark:text-[#94A3B8] mb-1">
                      QR Color
                    </label>
                    <div className="flex items-center gap-2">
                      <input
                        type="color"
                        value={fgColor}
                        onChange={(e) => setFgColor(e.target.value)}
                        className="w-7 h-7 rounded-[2px] border border-slate-300 dark:border-[#263448] cursor-pointer"
                      />
                      <span className="text-xs font-mono">{fgColor}</span>
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-medium text-slate-500 dark:text-[#94A3B8] mb-1">
                      Background Color
                    </label>
                    <div className="flex items-center gap-2">
                      <input
                        type="color"
                        value={bgColor}
                        onChange={(e) => setBgColor(e.target.value)}
                        className="w-7 h-7 rounded-[2px] border border-slate-300 dark:border-[#263448] cursor-pointer"
                      />
                      <span className="text-xs font-mono">{bgColor}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Right Column: Live QR Preview */}
            <div className="lg:col-span-5 bg-white dark:bg-[#141D2B] rounded-[2px] p-5 border border-slate-200 dark:border-[#263448] shadow-sm flex flex-col items-center text-center">
              <h3 className="font-semibold text-xs uppercase tracking-wider text-slate-900 dark:text-[#E8EDF5] mb-4">
                QR Code Preview
              </h3>

              <div className="p-3 rounded-[2px] bg-white border border-slate-200 shadow-sm mb-5">
                {qrDataUrl ? (
                  <img src={qrDataUrl} alt="QR Code" className="w-48 h-48 object-contain" />
                ) : (
                  <div className="w-48 h-48 flex items-center justify-center text-slate-400 text-xs">
                    Generating...
                  </div>
                )}
              </div>

              <div className="w-full space-y-2">
                <button
                  type="button"
                  onClick={handleDownloadPng}
                  className="w-full h-10 px-4 rounded-[2px] font-semibold text-xs text-white bg-[#3B82F6] hover:bg-[#2563EB] flex items-center justify-center gap-2 transition"
                >
                  <Download className="w-4 h-4" />
                  <span>Download PNG (HD)</span>
                </button>

                <button
                  type="button"
                  onClick={handleCopy}
                  className="w-full h-9 px-4 rounded-[2px] font-medium text-xs text-slate-700 dark:text-[#94A3B8] hover:text-slate-900 dark:hover:text-[#E8EDF5] bg-slate-100 dark:bg-[#111A29] border border-slate-200 dark:border-[#263448] flex items-center justify-center gap-1.5 transition"
                >
                  {copied ? (
                    <>
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                      <span>Copied Encoded Data!</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" />
                      <span>Copy Encoded Data</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        </section>

        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
