import React, { useState, useEffect } from 'react';
import { Volume2, VolumeX, Menu, X } from 'lucide-react';
import { useContent } from '../context/ContentContext';
import { StellerLogo } from './StellerLogo';
import { soundEngine } from '../utils/audioSystem';

interface NavbarProps {
  onLogoClick?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onLogoClick }) => {
  const { studioConfig } = useContent();
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [isAudioMuted, setIsAudioMuted] = useState(true);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleToggleAudio = () => {
    const isNowActive = soundEngine.toggleMute();
    setIsAudioMuted(!isNowActive);
  };

  const navLinks = [
    { label: 'PROJECTS', href: '#projects' },
    { label: 'ABOUT', href: '#about' },
    { label: 'CONTACT', href: '#contact' },
  ];

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        isScrolled
          ? 'bg-[#030509]/85 backdrop-blur-md border-b border-white/5 py-3 shadow-lg'
          : 'bg-transparent py-5'
      }`}
    >
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
        {/* Official Steller-Nebula Brand Identity */}
        <a
          href="#"
          onClick={(e) => {
            if (onLogoClick) {
              e.preventDefault();
              onLogoClick();
            }
            soundEngine.playButtonClick();
          }}
          className="group flex items-center gap-2.5 text-white focus:outline-none cursor-pointer"
          title="Steller-Nebula"
        >
          <StellerLogo size="sm" withGlow={false} />

          <span className="font-sans font-bold text-xs sm:text-sm tracking-[0.25em] text-white group-hover:text-cyan-300 transition-colors uppercase">
            {studioConfig.studioName}
          </span>
        </a>

        {/* Center / Right Desktop Navigation */}
        <div className="hidden md:flex items-center gap-8">
          <nav className="flex items-center gap-7">
            {navLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                onClick={() => soundEngine.playButtonClick()}
                onMouseEnter={() => soundEngine.playHoverBlip(820)}
                className="font-mono text-xs text-slate-100 hover:text-cyan-300 tracking-widest transition-colors relative py-1 font-semibold"
              >
                <span>{link.label}</span>
              </a>
            ))}
          </nav>

          {/* Clean Futuristic Audio Toggle Button: [🔊 AUDIO: OFF] */}
          <button
            onClick={handleToggleAudio}
            onMouseEnter={() => soundEngine.playHoverBlip(880)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md border font-mono text-[11px] tracking-wider transition-all duration-200 cursor-pointer ${
              !isAudioMuted
                ? 'border-cyan-400/50 bg-cyan-950/40 text-cyan-300 shadow-[0_0_12px_rgba(0,240,255,0.25)]'
                : 'border-white/10 bg-[#050C16]/60 text-slate-400 hover:text-slate-200 hover:border-white/20'
            }`}
            title={!isAudioMuted ? 'Mute audio' : 'Play audio'}
            aria-label="Toggle audio"
          >
            {!isAudioMuted ? (
              <>
                <Volume2 className="w-3.5 h-3.5 text-cyan-400" />
                <span>AUDIO: ON</span>
              </>
            ) : (
              <>
                <VolumeX className="w-3.5 h-3.5" />
                <span>AUDIO: OFF</span>
              </>
            )}
          </button>
        </div>

        {/* Mobile Hamburger Navigation */}
        <div className="flex md:hidden items-center gap-3">
          <button
            onClick={handleToggleAudio}
            className="p-2 rounded border border-white/10 text-slate-400 hover:text-white"
            aria-label="Toggle audio"
          >
            {!isAudioMuted ? (
              <Volume2 className="w-4 h-4 text-cyan-400" />
            ) : (
              <VolumeX className="w-4 h-4" />
            )}
          </button>

          <button
            onClick={() => {
              soundEngine.playButtonClick();
              setMobileMenuOpen(!mobileMenuOpen);
            }}
            className="p-2 rounded border border-white/10 text-slate-300 hover:text-white focus:outline-none"
            aria-label="Open menu"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-[#050811]/95 border-b border-white/10 px-6 py-6 space-y-4 backdrop-blur-xl animate-in slide-in-from-top-2 duration-200">
          <div className="flex flex-col space-y-3 font-mono text-xs">
            {navLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                onClick={() => {
                  soundEngine.playButtonClick();
                  setMobileMenuOpen(false);
                }}
                className="text-slate-300 hover:text-cyan-300 py-2 border-b border-white/5"
              >
                {link.label}
              </a>
            ))}
          </div>
        </div>
      )}
    </header>
  );
};
