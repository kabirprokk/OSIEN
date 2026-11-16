import React, { useEffect, useState } from 'react';

export const CustomCursor: React.FC = () => {
  const [position, setPosition] = useState({ x: -100, y: -100 });
  const [trail, setTrail] = useState({ x: -100, y: -100 });
  const [isVisible, setIsVisible] = useState(false);
  const [isMobile, setIsMobile] = useState(true);
  const [isHovered, setIsHovered] = useState(false);
  const [isClicking, setIsClicking] = useState(false);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(pointer: fine)');
    setIsMobile(!mediaQuery.matches);

    const handlePointerChange = (e: MediaQueryListEvent) => {
      setIsMobile(!e.matches);
    };

    mediaQuery.addEventListener('change', handlePointerChange);

    const handleMouseMove = (e: MouseEvent) => {
      setIsVisible(true);
      setPosition({ x: e.clientX, y: e.clientY });

      const target = e.target as HTMLElement | null;
      if (
        target &&
        target.closest('button, a, input, textarea, select, [role="button"], label, .cursor-pointer')
      ) {
        setIsHovered(true);
      } else {
        setIsHovered(false);
      }
    };

    const handleMouseDown = () => setIsClicking(true);
    const handleMouseUp = () => setIsClicking(false);
    const handleMouseLeave = () => setIsVisible(false);

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mousedown', handleMouseDown);
    window.addEventListener('mouseup', handleMouseUp);
    document.addEventListener('mouseleave', handleMouseLeave);

    return () => {
      mediaQuery.removeEventListener('change', handlePointerChange);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mousedown', handleMouseDown);
      window.removeEventListener('mouseup', handleMouseUp);
      document.removeEventListener('mouseleave', handleMouseLeave);
    };
  }, []);

  useEffect(() => {
    if (isMobile) return;
    let animationFrameId: number;

    const animateTrail = () => {
      setTrail((prev) => ({
        x: prev.x + (position.x - prev.x) * 0.18,
        y: prev.y + (position.y - prev.y) * 0.18,
      }));
      animationFrameId = requestAnimationFrame(animateTrail);
    };

    animationFrameId = requestAnimationFrame(animateTrail);
    return () => cancelAnimationFrame(animationFrameId);
  }, [position, isMobile]);

  if (isMobile || !isVisible) {
    return null;
  }

  return (
    <div className="pointer-events-none fixed inset-0 z-[9999] overflow-hidden select-none">
      {/* 1. Primary Precision Cursor (Instant position, styled in matching luminous ring format) */}
      <div
        className="fixed top-0 left-0 -translate-x-1/2 -translate-y-1/2 rounded-full pointer-events-none transition-[width,height,background-color,border-color,transform] duration-75 flex items-center justify-center"
        style={{
          transform: `translate3d(${position.x}px, ${position.y}px, 0) scale(${isClicking ? 0.75 : 1})`,
          width: isHovered ? '10px' : '6px',
          height: isHovered ? '10px' : '6px',
          backgroundColor: isHovered ? 'rgba(0, 240, 255, 0.9)' : '#00F0FF',
          border: '1px solid rgba(0, 240, 255, 0.95)',
          boxShadow: '0 0 10px #00F0FF, 0 0 18px rgba(0, 240, 255, 0.7)'
        }}
      />

      {/* 2. Outer Follower Ring (Smooth inertia trailing ring) */}
      <div
        className="fixed top-0 left-0 -translate-x-1/2 -translate-y-1/2 rounded-full pointer-events-none transition-[width,height,border-color,background-color,box-shadow,transform] duration-150 flex items-center justify-center"
        style={{
          transform: `translate3d(${trail.x}px, ${trail.y}px, 0) scale(${isClicking ? 0.85 : 1})`,
          width: isHovered ? '42px' : '26px',
          height: isHovered ? '42px' : '26px',
          border: isHovered
            ? '1.5px solid rgba(0, 240, 255, 0.9)'
            : '1px solid rgba(0, 240, 255, 0.4)',
          backgroundColor: isHovered ? 'rgba(0, 240, 255, 0.1)' : 'rgba(0, 240, 255, 0.02)',
          boxShadow: isHovered
            ? '0 0 20px rgba(0, 240, 255, 0.35), inset 0 0 10px rgba(0, 240, 255, 0.15)'
            : '0 0 10px rgba(0, 240, 255, 0.1)'
        }}
      />
    </div>
  );
};

