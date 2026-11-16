import React, { useState, useEffect } from 'react';
import { CosmicBackground } from './components/CosmicBackground';
import { CustomCursor } from './components/CustomCursor';
import { Navbar } from './components/Navbar';
import { HeroSection } from './components/HeroSection';
import { ProjectsUniverse } from './components/ProjectsUniverse';
import { AboutSection } from './components/AboutSection';
import { ContactSection } from './components/ContactSection';
import { Footer } from './components/Footer';
import { EasterEggOverlay } from './components/EasterEggOverlay';
import { ContentProvider } from './context/ContentContext';
import { ScrollJourneyProvider } from './context/ScrollJourneyContext';
import { OrbitalNavigator } from './components/OrbitalNavigator';
import { AdminPanel } from './components/admin/AdminPanel';
import { soundEngine } from './utils/audioSystem';

function MainAppContent() {
  const [showLogoToast, setShowLogoToast] = useState(false);
  const [starBurstCount, setStarBurstCount] = useState(0);
  const [isAdminOpen, setIsAdminOpen] = useState(false);

  // Hidden admin keyboard shortcut & hash trigger
  useEffect(() => {
    // Check initial hash or path (e.g. #admin or /admin)
    if (
      window.location.hash === '#admin' ||
      window.location.pathname.endsWith('/admin')
    ) {
      setIsAdminOpen(true);
    }

    const handleKeyDown = (e: KeyboardEvent) => {
      // Secret key combination: Ctrl + Shift + A (or Cmd + Shift + A on macOS)
      if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'a' || e.key === 'A')) {
        e.preventDefault();
        soundEngine.playEasterEggFanfare();
        setIsAdminOpen((prev) => !prev);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const handleStarClick = (count: number) => {
    setStarBurstCount(count);
  };

  const handleExploreClick = () => {
    const el = document.getElementById('projects');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleCoreMarkClick = () => {
    setShowLogoToast(true);
    setTimeout(() => setShowLogoToast(false), 3500);
  };

  return (
    <div className="relative min-h-screen bg-[#05060A] text-slate-100 selection:bg-cyan-500/30 selection:text-cyan-200 antialiased overflow-x-hidden font-sans">
      {/* 1. Continuous Scroll-Driven Cosmic Background Canvas */}
      <CosmicBackground onStarClick={handleStarClick} />

      {/* 2. Precision Custom Cursor */}
      <CustomCursor />

      {/* 3. Global Minimal Navigation */}
      <Navbar onLogoClick={handleCoreMarkClick} />

      {/* 4. Subtle Orbital Progress Indicator & Chapter Tracker */}
      <OrbitalNavigator />

      {/* Main Content Journey Sections */}
      <main className="relative z-10">
        {/* 01 // ORIGIN: Cinematic Hero & Depth Travel */}
        <HeroSection
          onExploreClick={handleExploreClick}
          onMarkClick={handleCoreMarkClick}
        />

        {/* 02 // PROJECTS: Storyteller Orbital Encounter */}
        <ProjectsUniverse />

        {/* 03 // ABOUT: Deep Space Philosophy Expanse */}
        <AboutSection />

        {/* 04 // CONTACT: Return Signal Beacon */}
        <ContactSection />
      </main>

      {/* Minimalist Footer */}
      <Footer />

      {/* Subtle Interactive Easter Egg Overlays */}
      <EasterEggOverlay
        showLogoToast={showLogoToast}
        onCloseLogoToast={() => setShowLogoToast(false)}
        starBurstCount={starBurstCount}
      />

      {/* Hidden Admin / Content Editor Panel (Ctrl + Shift + A) */}
      <AdminPanel
        isOpen={isAdminOpen}
        onClose={() => setIsAdminOpen(false)}
      />
    </div>
  );
}

export default function App() {
  return (
    <ContentProvider>
      <ScrollJourneyProvider>
        <MainAppContent />
      </ScrollJourneyProvider>
    </ContentProvider>
  );
}

