import React from 'react';
import { ArrowUp } from 'lucide-react';
import { useContent } from '../context/ContentContext';
import { StellerLogo } from './StellerLogo';
import { soundEngine } from '../utils/audioSystem';

export const Footer: React.FC = () => {
  const { studioConfig } = useContent();

  const scrollToTop = () => {
    soundEngine.playButtonClick();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="relative border-t border-white/5 bg-[#030509] text-slate-400 font-mono text-xs pt-12 pb-12 px-4 sm:px-6 lg:px-8 select-none">
      <div className="max-w-5xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-6">
        {/* Brand & Copyright */}
        <div className="flex items-center gap-3 text-center sm:text-left">
          <StellerLogo size="sm" withGlow={false} />
          <div className="space-y-0.5">
            <div className="font-sans font-bold text-xs text-white tracking-[0.25em] uppercase">
              {studioConfig.studioName}
            </div>
            <div className="text-slate-500 text-[10px]">
              © {studioConfig.year} {studioConfig.studioName}. All rights reserved.
            </div>
          </div>
        </div>

        {/* Links and Return to Top */}
        <div className="flex items-center gap-6">
          <a
            href="#projects"
            onClick={() => soundEngine.playButtonClick()}
            className="hover:text-cyan-300 transition-colors text-[10px] tracking-wider"
          >
            PROJECTS
          </a>
          <a
            href="#about"
            onClick={() => soundEngine.playButtonClick()}
            className="hover:text-cyan-300 transition-colors text-[10px] tracking-wider"
          >
            ABOUT
          </a>
          <a
            href="#contact"
            onClick={() => soundEngine.playButtonClick()}
            className="hover:text-cyan-300 transition-colors text-[10px] tracking-wider"
          >
            CONTACT
          </a>

          <button
            onClick={scrollToTop}
            onMouseEnter={() => soundEngine.playHoverBlip(1100)}
            className="p-2 rounded border border-white/10 hover:border-cyan-400 text-slate-400 hover:text-cyan-300 bg-[#050811] transition-all cursor-pointer"
            title="Return to top"
            aria-label="Scroll back to top"
          >
            <ArrowUp className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </footer>
  );
};
