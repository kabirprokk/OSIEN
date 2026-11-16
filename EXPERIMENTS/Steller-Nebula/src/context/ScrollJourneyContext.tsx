import React, { createContext, useContext, useEffect, useState, useRef, useMemo } from 'react';

export type ChapterId = 'origin' | 'projects' | 'about' | 'contact';

export interface ChapterInfo {
  id: ChapterId;
  index: string;
  name: string;
  subtitle: string;
  themeColor: string;
}

export const CHAPTERS: ChapterInfo[] = [
  { id: 'origin', index: '01', name: 'HOME', subtitle: 'TOP', themeColor: '#00F0FF' },
  { id: 'projects', index: '02', name: 'PROJECTS', subtitle: 'WORK', themeColor: '#38BDF8' },
  { id: 'about', index: '03', name: 'ABOUT', subtitle: 'STUDIO', themeColor: '#A855F7' },
  { id: 'contact', index: '04', name: 'CONTACT', subtitle: 'CONNECT', themeColor: '#2DD4BF' },
];

interface ScrollJourneyState {
  scrollY: number;
  smoothScrollY: number;
  progress: number;
  smoothProgress: number;
  velocity: number;
  smoothVelocity: number;
  activeChapter: ChapterId;
  activeChapterInfo: ChapterInfo;
  sectionProgress: {
    origin: number;
    projects: number;
    about: number;
    contact: number;
  };
  isScrolling: boolean;
  scrollToChapter: (id: ChapterId) => void;
}

const ScrollJourneyContext = createContext<ScrollJourneyState | null>(null);

export const ScrollJourneyProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [scrollY, setScrollY] = useState(0);
  const [smoothScrollY, setSmoothScrollY] = useState(0);
  const [progress, setProgress] = useState(0);
  const [smoothProgress, setSmoothProgress] = useState(0);
  const [velocity, setVelocity] = useState(0);
  const [smoothVelocity, setSmoothVelocity] = useState(0);
  const [activeChapter, setActiveChapter] = useState<ChapterId>('origin');
  const [isScrolling, setIsScrolling] = useState(false);

  const [sectionProgress, setSectionProgress] = useState({
    origin: 0,
    projects: 0,
    about: 0,
    contact: 0,
  });

  const stateRef = useRef({
    currentY: 0,
    targetY: 0,
    lastY: 0,
    lastTime: performance.now(),
    currentVelocity: 0,
    targetVelocity: 0,
    scrollTimeout: 0 as unknown as NodeJS.Timeout,
  });

  useEffect(() => {
    let animationFrameId: number;

    const calculateSectionProgress = (y: number, docHeight: number, winHeight: number) => {
      const getProgressForElement = (id: string) => {
        const el = document.getElementById(id);
        if (!el) return 0;
        const rect = el.getBoundingClientRect();
        const top = rect.top + window.scrollY;
        const height = rect.height;
        // 0 when top enters viewport from bottom, 0.5 when centered, 1 when bottom leaves top
        const p = (y + winHeight - top) / (height + winHeight);
        return Math.max(0, Math.min(1, p));
      };

      const pOrigin = getProgressForElement('origin');
      const pProjects = getProgressForElement('projects');
      const pAbout = getProgressForElement('about');
      const pContact = getProgressForElement('contact');

      setSectionProgress({
        origin: pOrigin,
        projects: pProjects,
        about: pAbout,
        contact: pContact,
      });

      // Determine active chapter based on viewport center
      const viewCenter = y + winHeight * 0.45;
      const originEl = document.getElementById('origin');
      const projEl = document.getElementById('projects');
      const aboutEl = document.getElementById('about');
      const contactEl = document.getElementById('contact');

      if (contactEl && viewCenter >= contactEl.offsetTop - 100) {
        setActiveChapter('contact');
      } else if (aboutEl && viewCenter >= aboutEl.offsetTop - 100) {
        setActiveChapter('about');
      } else if (projEl && viewCenter >= projEl.offsetTop - 100) {
        setActiveChapter('projects');
      } else {
        setActiveChapter('origin');
      }
    };

    const handleScroll = () => {
      const currentScroll = window.scrollY || window.pageYOffset;
      const maxScroll = Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
      const rawProgress = Math.max(0, Math.min(1, currentScroll / maxScroll));

      const now = performance.now();
      const dt = Math.max(10, now - stateRef.current.lastTime);
      const dy = currentScroll - stateRef.current.lastY;
      const rawVelocity = (dy / dt) * 16.66; // Normalized to 60fps pixels/frame

      stateRef.current.targetY = currentScroll;
      stateRef.current.targetVelocity = Math.max(-100, Math.min(100, rawVelocity));
      stateRef.current.lastY = currentScroll;
      stateRef.current.lastTime = now;

      setScrollY(currentScroll);
      setProgress(rawProgress);
      setVelocity(rawVelocity);
      setIsScrolling(true);

      clearTimeout(stateRef.current.scrollTimeout);
      stateRef.current.scrollTimeout = setTimeout(() => {
        setIsScrolling(false);
      }, 150);

      calculateSectionProgress(currentScroll, document.documentElement.scrollHeight, window.innerHeight);
    };

    // Smooth Lerp animation loop for physical momentum
    const loop = () => {
      const { targetY, targetVelocity } = stateRef.current;
      
      // Interpolate smooth scroll Y
      stateRef.current.currentY += (targetY - stateRef.current.currentY) * 0.12;
      // Interpolate smooth velocity (damping)
      stateRef.current.currentVelocity += (targetVelocity - stateRef.current.currentVelocity) * 0.1;
      
      // Decay target velocity if no scroll
      stateRef.current.targetVelocity *= 0.88;

      setSmoothScrollY(stateRef.current.currentY);
      setSmoothVelocity(stateRef.current.currentVelocity);

      const maxScroll = Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
      setSmoothProgress(Math.max(0, Math.min(1, stateRef.current.currentY / maxScroll)));

      animationFrameId = requestAnimationFrame(loop);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    window.addEventListener('resize', handleScroll, { passive: true });
    
    // Initial calculate
    handleScroll();
    animationFrameId = requestAnimationFrame(loop);

    return () => {
      window.removeEventListener('scroll', handleScroll);
      window.removeEventListener('resize', handleScroll);
      cancelAnimationFrame(animationFrameId);
      clearTimeout(stateRef.current.scrollTimeout);
    };
  }, []);

  const scrollToChapter = (id: ChapterId) => {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const activeChapterInfo = useMemo(() => {
    return CHAPTERS.find((c) => c.id === activeChapter) || CHAPTERS[0];
  }, [activeChapter]);

  return (
    <ScrollJourneyContext.Provider
      value={{
        scrollY,
        smoothScrollY,
        progress,
        smoothProgress,
        velocity,
        smoothVelocity,
        activeChapter,
        activeChapterInfo,
        sectionProgress,
        isScrolling,
        scrollToChapter,
      }}
    >
      {children}
    </ScrollJourneyContext.Provider>
  );
};

export const useScrollJourney = (): ScrollJourneyState => {
  const context = useContext(ScrollJourneyContext);
  if (!context) {
    throw new Error('useScrollJourney must be used within a ScrollJourneyProvider');
  }
  return context;
};
