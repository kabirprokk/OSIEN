import React, { useState, useEffect } from 'react';
import { Sparkles, Zap, X } from 'lucide-react';
import { soundEngine } from '../utils/audioSystem';

interface EasterEggOverlayProps {
  showLogoToast: boolean;
  onCloseLogoToast: () => void;
  starBurstCount: number;
}

export const EasterEggOverlay: React.FC<EasterEggOverlayProps> = ({
  showLogoToast,
  onCloseLogoToast,
  starBurstCount
}) => {
  const [konamiUnlocked, setKonamiUnlocked] = useState(false);
  const [starToast, setStarToast] = useState<string | null>(null);

  // Konami Code sequence listener
 useEffect(() => {
  const konamiSequence = [
    'ArrowUp',
    'ArrowUp',
    'ArrowDown',
    'ArrowDown',
  ];

  let sequenceIndex = 0;
  let resetTimer: ReturnType<typeof setTimeout> | null = null;

  const resetSequence = () => {
    sequenceIndex = 0;

    if (resetTimer) {
      clearTimeout(resetTimer);
      resetTimer = null;
    }
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.ctrlKey || e.altKey || e.metaKey || e.repeat) {
      return;
    }

    const key = e.key;

    if (key === konamiSequence[sequenceIndex]) {
      sequenceIndex++;

      // Give the user 3 seconds to finish the sequence.
      if (resetTimer) {
        clearTimeout(resetTimer);
      }

      resetTimer = setTimeout(() => {
        sequenceIndex = 0;
      }, 3000);

      if (sequenceIndex === konamiSequence.length) {
        resetSequence();

        setKonamiUnlocked(true);
        soundEngine.playEasterEggFanfare();
      }
    } else {
      // If they press ArrowUp, treat it as the beginning again.
      sequenceIndex = key === konamiSequence[0] ? 1 : 0;

      if (resetTimer) {
        clearTimeout(resetTimer);
      }

      if (sequenceIndex > 0) {
        resetTimer = setTimeout(() => {
          sequenceIndex = 0;
        }, 3000);
      }
    }
  };

  window.addEventListener('keydown', handleKeyDown);

  return () => {
    window.removeEventListener('keydown', handleKeyDown);

    if (resetTimer) {
      clearTimeout(resetTimer);
    }
  };
}, []);

  // Subtle toast after repeated star bursts
  useEffect(() => {
    if (starBurstCount > 0 && starBurstCount % 5 === 0) {
      setStarToast('Thanks for stopping by.');
      const timer = setTimeout(() => setStarToast(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [starBurstCount]);

  return (
    <>
      {/* 1. Konami Code Modal */}
      {konamiUnlocked && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-xl animate-in fade-in duration-300">
          <div className="relative w-full max-w-md p-8 rounded-2xl border border-cyan-400/50 bg-[#080B12] text-center shadow-[0_0_60px_rgba(0,240,255,0.3)] space-y-5">
            <div className="w-12 h-12 rounded-full bg-cyan-500/20 border border-cyan-400 mx-auto flex items-center justify-center text-cyan-300">
              <Sparkles className="w-6 h-6 animate-pulse" />
            </div>

            <div>
              <span className="font-mono text-[11px] text-cyan-400 tracking-widest uppercase block">
                SECRET FOUND
              </span>
              <h3 className="font-sans font-bold text-xl text-white mt-1">
                Thanks for exploring.
              </h3>
            </div>

            <p className="text-xs font-sans text-slate-300 leading-relaxed">
              Every detail here was handcrafted with care and intent. Thank you for visiting Steller-Nebula.
            </p>

            <button
              onClick={() => {
                soundEngine.playButtonClick();
                setKonamiUnlocked(false);
              }}
              className="px-5 py-2 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-mono text-xs font-bold tracking-wider uppercase transition-all"
            >
              CONTINUE
            </button>
          </div>
        </div>
      )}

      {/* 2. Logo Toast */}
      {showLogoToast && (
        <div className="fixed bottom-6 right-6 z-40 px-4 py-2.5 rounded-lg bg-[#080B12]/95 border border-cyan-400/50 font-mono text-xs text-cyan-300 shadow-[0_0_20px_rgba(0,240,255,0.2)] flex items-center gap-3 animate-in slide-in-from-bottom duration-200">
          <Sparkles className="w-4 h-4 text-cyan-400" />
          <span>You found a secret.</span>
          <button
            onClick={onCloseLogoToast}
            className="text-slate-400 hover:text-white ml-2"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* 3. Star Resonance Toast */}
      {starToast && (
        <div className="fixed bottom-6 right-6 z-40 px-4 py-2.5 rounded-lg bg-[#080B12]/95 border border-cyan-400/50 font-mono text-xs text-cyan-300 shadow-[0_0_20px_rgba(0,240,255,0.2)] flex items-center gap-2 animate-in slide-in-from-bottom duration-200">
          <Sparkles className="w-4 h-4 text-cyan-400" />
          <span>{starToast}</span>
        </div>
      )}
    </>
  );
};
