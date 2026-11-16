import React from 'react';
import { CHAPTERS, ChapterId, useScrollJourney } from '../context/ScrollJourneyContext';
import { soundEngine } from '../utils/audioSystem';

export const OrbitalNavigator: React.FC = () => {
  const { activeChapter, scrollToChapter } = useScrollJourney();

  const handleChapterClick = (id: ChapterId) => {
    soundEngine.playButtonClick();
    scrollToChapter(id);
  };

  return (
    <aside
      aria-label="Section navigation"
      className="fixed right-4 sm:right-8 top-1/2 -translate-y-1/2 z-40 hidden lg:flex flex-col items-start pointer-events-auto select-none space-y-6"
    >
      {/* Continuous Vertical Connecting Line */}
      <div className="absolute left-[3px] top-2 bottom-2 w-[1px] bg-white/10 pointer-events-none" />

      {CHAPTERS.map((ch, idx) => {
        const isActive = activeChapter === ch.id;

        return (
          <button
            key={ch.id}
            onClick={() => handleChapterClick(ch.id)}
            onMouseEnter={() => soundEngine.playHoverBlip(750 + idx * 90)}
            className="group relative flex items-start gap-3 text-left focus:outline-none cursor-pointer"
            aria-label={`Jump to ${ch.name}`}
          >
            {/* Indicator Dot / Bead */}
            <div className="relative mt-1 z-10 flex items-center justify-center">
              <div
                className={`w-2 h-2 rounded-full transition-all duration-300 ${
                  isActive
                    ? 'bg-cyan-400 shadow-[0_0_10px_#00F0FF] ring-2 ring-cyan-400/30'
                    : 'bg-slate-700 group-hover:bg-slate-400'
                }`}
              />
            </div>

            {/* Label Block: 01 / NAME */}
            <div className="flex flex-col">
              <span
                className={`font-mono text-[10px] tracking-widest transition-colors duration-200 ${
                  isActive ? 'text-cyan-400 font-bold' : 'text-slate-500 group-hover:text-slate-300'
                }`}
              >
                {ch.index}
              </span>
              <span
                className={`font-mono text-[9px] tracking-wider transition-colors duration-200 uppercase ${
                  isActive ? 'text-white font-medium' : 'text-slate-600 group-hover:text-slate-400'
                }`}
              >
                {ch.name}
              </span>
            </div>
          </button>
        );
      })}
    </aside>
  );
};
