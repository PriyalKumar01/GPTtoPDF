'use client';

import React, { useState, useRef } from 'react';
import { Navbar } from '@/components/Navbar';
import { ConverterHero } from '@/components/ConverterHero';
import { ConversationPreview } from '@/components/ConversationPreview';
import { CollegeStudyPromo } from '@/components/CollegeStudyPromo';
import { Footer } from '@/components/Footer';
import { ConversationData } from '@/lib/types';

export default function HomePage() {
  const [conversation, setConversation] = useState<ConversationData | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const previewRef = useRef<HTMLDivElement>(null);

  const handleConversationReady = (data: ConversationData) => {
    setConversation(data);
    setTimeout(() => {
      previewRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
  };

  const handleReset = () => {
    setConversation(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar />

      <main className="flex-1">
        {/* Hero & Converter Input */}
        <ConverterHero
          onConversationReady={handleConversationReady}
          isProcessing={isProcessing}
          setIsProcessing={setIsProcessing}
        />

        {/* Conversation Preview & PDF Customizer */}
        {conversation && (
          <div ref={previewRef}>
            <ConversationPreview
              conversation={conversation}
              onReset={handleReset}
            />
          </div>
        )}

        {/* Partnered with College Study Promotion */}
        <CollegeStudyPromo />
      </main>

      <Footer />
    </div>
  );
}
