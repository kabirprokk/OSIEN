import React, { useState } from 'react';
import { ArrowDown } from 'lucide-react';
import { useContent } from '../context/ContentContext';
import { useScrollJourney } from '../context/ScrollJourneyContext';
import { StellerLogo } from './StellerLogo';
import { soundEngine } from '../utils/audioSystem';

interface HeroSectionProps {
  onExploreClick: () => void;
  onMarkClick?: () => void;
}

export const HeroSection: React.FC<HeroSectionProps> = ({ onExploreClick, onMarkClick }) => {
  const { studioConfig } = useContent();
  const { sectionProgress, smoothScrollY } = useScrollJourney();
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

  // Section progress (0 when centered, increases as scrolled down)
  const p = sectionProgress.origin || 0;
  const depthScale = Math.max(0.7, 1 - p * 0.4);
  const depthTranslateY = smoothScrollY * 0.22;
  const opacity = Math.max(0, 1 - p * 1.25);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    setMousePos({ x, y });
  };

  const handleLogoClick = () => {
    soundEngine.playSupernovaBurst();
    if (onMarkClick) onMarkClick();
  };

  return (
    <section
      id="origin"
      onMouseMove={handleMouseMove}
      className="relative min-h-[100vh] flex flex-col items-center justify-center px-4 sm:px-6 lg:px-8 pt-20 pb-16 max-w-5xl mx-auto text-center select-none overflow-hidden"
    >
      {/* 1. Official Steller-Nebula Crystal S Core Emblem with Orbital Geometry */}
      <div
        className="relative mb-6 sm:mb-8 will-change-transform transition-all duration-100 ease-out flex items-center justify-center"
        style={{
          transform: `translate3d(${mousePos.x * 16}px, ${depthTranslateY + mousePos.y * 16}px, 0) scale(${depthScale})`,
          opacity,
        }}
      >
        <StellerLogo
          size="hero"
          withGlow={true}
          withRings={true}
          interactive={true}
          onClick={handleLogoClick}
        />
      </div>

      {/* 2. Hero Headline & Spaced Futuristic Typography */}
      <div
        className="space-y-4 max-w-3xl will-change-transform transition-all duration-100"
        style={{
          transform: `translate3d(0, ${depthTranslateY * 0.5}px, 0)`,
          opacity,
        }}
      >
        <h1 className="text-3xl sm:text-5xl md:text-6xl font-sans font-black tracking-[0.22em] text-white uppercase select-none drop-shadow-[0_0_25px_rgba(255,255,255,0.9)] drop-shadow-[0_0_55px_rgba(0,240,255,0.55)]">
          {studioConfig.heroHeadline}
        </h1>

        <p className="font-sans font-bold text-base sm:text-xl text-[#67E8F9] tracking-tight drop-shadow-[0_0_22px_rgba(0,240,255,0.95)]">
          {studioConfig.heroSubtitle}
        </p>

        <p className="font-sans font-medium text-sm sm:text-base text-slate-100 max-w-xl mx-auto leading-relaxed drop-shadow-[0_2px_14px_rgba(0,0,0,0.95)]">
          {studioConfig.heroSupportingText}
        </p>
      </div>

      {/* 3. Sleek Cyan Capsule Button: ENTER DISCOVERY MATRIX */}
      <div
        className="mt-8 sm:mt-10 flex flex-col items-center gap-6 will-change-transform"
        style={{
          transform: `translate3d(0, ${depthTranslateY * 0.3}px, 0)`,
          opacity,
        }}
      >
        <button
          onClick={() => {
            soundEngine.playButtonClick();
            onExploreClick();
          }}
          onMouseEnter={() => soundEngine.playHoverBlip(880)}
          className="group inline-flex items-center gap-2 px-7 sm:px-9 py-3.5 rounded-full border border-cyan-400/60 hover:border-cyan-300 bg-[#051324]/80 hover:bg-[#08223d]/90 backdrop-blur-md text-cyan-200 hover:text-white font-mono text-xs tracking-widest uppercase transition-all duration-300 shadow-[0_0_25px_rgba(0,240,255,0.25)] hover:shadow-[0_0_40px_rgba(0,240,255,0.55)] cursor-pointer"
        >
          <span className="font-bold">VIEW PROJECTS</span>
          <ArrowDown className="w-3.5 h-3.5 text-cyan-300 group-hover:text-white group-hover:translate-y-0.5 transition-transform duration-200" />
        </button>

        {/* 4. Scroll Indicator */}
        <div className="flex flex-col items-center gap-2 opacity-80 hover:opacity-100 transition-opacity">
          <div className="w-1.5 h-1.5 rounded-full bg-cyan-400 shadow-[0_0_10px_#00F0FF] animate-pulse" />
          <div className="w-[1px] h-6 bg-gradient-to-b from-cyan-400/80 to-transparent" />
          <span className="font-mono text-[9px] text-cyan-300 tracking-[0.2em] uppercase font-semibold">
            SCROLL TO EXPLORE
          </span>
        </div>
      </div>
    </section>
  );
};
