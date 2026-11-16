import React, { useEffect, useRef } from 'react';
import { useScrollJourney } from '../context/ScrollJourneyContext';
import { soundEngine } from '../utils/audioSystem';

interface Star {
  x: number;
  y: number;
  z: number;
  size: number;
  baseAlpha: number;
  alpha: number;
  twinkleSpeed: number;
  twinklePhase: number;
  hasGlint?: boolean;
  color: string;
}

interface SupernovaParticle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  color: string;
  alpha: number;
  life: number;
  maxLife: number;
}

interface CosmicBackgroundProps {
  onStarClick?: (count: number) => void;
}

export const CosmicBackground: React.FC<CosmicBackgroundProps> = ({ onStarClick }) => {
  const { smoothScrollY, smoothVelocity, activeChapter } = useScrollJourney();
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const mouseRef = useRef<{ x: number; y: number; targetX: number; targetY: number }>({
    x: window.innerWidth / 2,
    y: window.innerHeight / 2,
    targetX: window.innerWidth / 2,
    targetY: window.innerHeight / 2,
  });

  const starsRef = useRef<Star[]>([]);
  const burstsRef = useRef<SupernovaParticle[]>([]);
  const animationFrameId = useRef<number | null>(null);

  const scrollMetricsRef = useRef({
    scrollY: 0,
    velocity: 0,
    activeChapter: 'origin',
  });

  useEffect(() => {
    scrollMetricsRef.current.scrollY = smoothScrollY;
    scrollMetricsRef.current.velocity = smoothVelocity;
    scrollMetricsRef.current.activeChapter = activeChapter;
  }, [smoothScrollY, smoothVelocity, activeChapter]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d', { alpha: false });
    if (!ctx) return;

    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    // Initialize refined, balanced starfield with plenty of dark negative space
    const initStars = () => {
      const isMobile = width < 768;
      const count = isMobile ? 65 : 120; // Deliberate restraint
      const stars: Star[] = [];
      const colors = ['#FFFFFF', '#D1F4FF', '#E2D9F3', '#00F0FF', '#7DD3FC'];

      for (let i = 0; i < count; i++) {
        const isFar = i < count * 0.7; // 70% far dim stars
        const isFeature = !isFar && Math.random() < 0.12; // Rare feature glint stars

        const z = isFar ? Math.random() * 0.5 + 0.2 : Math.random() * 1.0 + 0.8;
        const size = isFar
          ? Math.random() * 0.8 + 0.4
          : isFeature
          ? Math.random() * 1.5 + 1.2
          : Math.random() * 1.0 + 0.6;

        const baseAlpha = isFar
          ? Math.random() * 0.3 + 0.1
          : isFeature
          ? Math.random() * 0.4 + 0.6
          : Math.random() * 0.4 + 0.3;

        stars.push({
          x: Math.random() * width,
          y: Math.random() * (height * 3), // virtual vertical depth
          z,
          size,
          baseAlpha,
          alpha: baseAlpha,
          twinkleSpeed: Math.random() * 0.015 + 0.005,
          twinklePhase: Math.random() * Math.PI * 2,
          hasGlint: isFeature,
          color: colors[Math.floor(Math.random() * colors.length)],
        });
      }
      starsRef.current = stars;
    };

    initStars();

    const handleResize = () => {
      if (!canvas) return;
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
      initStars();
    };

    const handleMouseMove = (e: MouseEvent) => {
      mouseRef.current.targetX = e.clientX;
      mouseRef.current.targetY = e.clientY;
    };

    window.addEventListener('resize', handleResize);
    window.addEventListener('mousemove', handleMouseMove);

    let time = 0;
    const render = () => {
      time += 0.008;

      const { scrollY, velocity, activeChapter } = scrollMetricsRef.current;

      // Mouse parallax smooth interpolation
      mouseRef.current.x += (mouseRef.current.targetX - mouseRef.current.x) * 0.04;
      mouseRef.current.y += (mouseRef.current.targetY - mouseRef.current.y) * 0.04;

      const mouseParallaxX = (mouseRef.current.x - width / 2) * 0.02;
      const mouseParallaxY = (mouseRef.current.y - height / 2) * 0.02;

      // 1. Deepest Background Fill (Near-black / dark cosmic navy)
      ctx.fillStyle = '#030509';
      ctx.fillRect(0, 0, width, height);

      // 2. Subtle Global Radial Vignette (Deep Blue undertones in center-left)
      const centerGlow = ctx.createRadialGradient(
        width * 0.48 + mouseParallaxX * 10,
        height * 0.45 + mouseParallaxY * 10,
        50,
        width * 0.48,
        height * 0.45,
        width * 0.7
      );
      centerGlow.addColorStop(0, 'rgba(8, 22, 45, 0.28)');
      centerGlow.addColorStop(0.5, 'rgba(4, 12, 28, 0.15)');
      centerGlow.addColorStop(1, 'rgba(3, 5, 9, 0)');
      ctx.fillStyle = centerGlow;
      ctx.fillRect(0, 0, width, height);

      // 3. Subtle Cosmic Violet / Indigo Nebula Cloud on Right Midground (Matching reference image)
      const nebulaViolet = ctx.createRadialGradient(
        width * 0.82 - mouseParallaxX * 8,
        height * 0.52 - (scrollY * 0.08) % height - mouseParallaxY * 8,
        20,
        width * 0.82,
        height * 0.52 - (scrollY * 0.08) % height,
        width * 0.38
      );
      nebulaViolet.addColorStop(0, 'rgba(124, 58, 237, 0.08)');
      nebulaViolet.addColorStop(0.4, 'rgba(88, 28, 135, 0.04)');
      nebulaViolet.addColorStop(1, 'rgba(3, 5, 9, 0)');
      ctx.fillStyle = nebulaViolet;
      ctx.fillRect(0, 0, width, height);

      // 4. Subtle Cosmic Dust (Restrained, deep space)
      const nebulaCyan = ctx.createRadialGradient(
        width * 0.12 + mouseParallaxX * 6,
        height * 0.38 - (scrollY * 0.08) % height + mouseParallaxY * 6,
        10,
        width * 0.12,
        height * 0.38 - (scrollY * 0.08) % height,
        width * 0.18
      );
      nebulaCyan.addColorStop(0, 'rgba(0, 240, 255, 0.02)');
      nebulaCyan.addColorStop(1, 'rgba(3, 5, 9, 0)');
      ctx.fillStyle = nebulaCyan;
      ctx.fillRect(0, 0, width, height);

      // 5. Large Continuous Orbit with Thin Cyan-to-Purple Gradient Outline
      const isMobile = width < 768;
      const orbitRadius = isMobile ? Math.max(height * 0.95, width * 1.15) : Math.max(width * 0.78, height * 1.12);
      const orbitCenterX = isMobile ? -orbitRadius * 0.68 + mouseParallaxX * 4 : -width * 0.32 + mouseParallaxX * 6;
      const orbitCenterY = height * 0.48 + (scrollY * 0.02) + mouseParallaxY * 6;

      ctx.save();

      // Clean dark celestial shadow inside the circle (left side)
      const darkOrbitGradient = ctx.createRadialGradient(
        orbitCenterX,
        orbitCenterY,
        orbitRadius * 0.2,
        orbitCenterX,
        orbitCenterY,
        orbitRadius
      );
      darkOrbitGradient.addColorStop(0, 'rgba(2, 4, 8, 0.94)');
      darkOrbitGradient.addColorStop(0.85, 'rgba(3, 6, 14, 0.88)');
      darkOrbitGradient.addColorStop(1, 'rgba(3, 5, 9, 0.2)');

      ctx.beginPath();
      ctx.arc(orbitCenterX, orbitCenterY, orbitRadius, 0, Math.PI * 2);
      ctx.fillStyle = darkOrbitGradient;
      ctx.fill();

      // Ultra-thin cyan fade hugging directly along the inner edge of the outline (~15px narrow band)
      const tightCyanFade = ctx.createRadialGradient(
        orbitCenterX,
        orbitCenterY,
        orbitRadius * 0.975,
        orbitCenterX,
        orbitCenterY,
        orbitRadius
      );
      tightCyanFade.addColorStop(0, 'rgba(0, 240, 255, 0)');
      tightCyanFade.addColorStop(0.5, 'rgba(0, 240, 255, 0.035)');
      tightCyanFade.addColorStop(1, 'rgba(0, 240, 255, 0.08)');

      ctx.beginPath();
      ctx.arc(orbitCenterX, orbitCenterY, orbitRadius, 0, Math.PI * 2);
      ctx.fillStyle = tightCyanFade;
      ctx.fill();

      // Gentle left-edge vignette for atmospheric depth (narrower width)
      const leftEdgeShadow = ctx.createLinearGradient(0, 0, width * 0.18, 0);
      leftEdgeShadow.addColorStop(0, 'rgba(2, 4, 8, 0.5)');
      leftEdgeShadow.addColorStop(1, 'rgba(3, 5, 9, 0)');
      ctx.fillStyle = leftEdgeShadow;
      ctx.fillRect(0, 0, width * 0.18, height);

      // Dynamic Gradient mapped across the visible arc that moves continuously with scroll
      const scrollLoopLength = height * 1.8;
      const scrollOffset = (scrollY * 0.55) % scrollLoopLength;

      const orbitStrokeGradient = ctx.createLinearGradient(
        width * 0.1,
        -scrollOffset,
        width * 0.45,
        scrollLoopLength - scrollOffset
      );
      orbitStrokeGradient.addColorStop(0, '#C084FC');      // Purple
      orbitStrokeGradient.addColorStop(0.16, '#A855F7');   // Vivid Purple
      orbitStrokeGradient.addColorStop(0.33, '#00F0FF');   // Electric Cyan
      orbitStrokeGradient.addColorStop(0.5, '#38BDF8');    // Sky Blue
      orbitStrokeGradient.addColorStop(0.66, '#C084FC');   // Purple
      orbitStrokeGradient.addColorStop(0.83, '#00F0FF');   // Electric Cyan
      orbitStrokeGradient.addColorStop(1, '#A855F7');      // Vivid Purple

      // Single ultra-thin, razor-sharp gradient line (0.6px thin outline)
      ctx.beginPath();
      ctx.arc(orbitCenterX, orbitCenterY, orbitRadius, 0, Math.PI * 2);
      ctx.strokeStyle = orbitStrokeGradient;
      ctx.lineWidth = 0.6;
      ctx.shadowBlur = 0;
      ctx.stroke();

      ctx.restore();

      // 7. Starfield Rendering (Layered depths, subtle glints, velocity stretch)
      const stars = starsRef.current;
      const velocityStretch = Math.max(-8, Math.min(8, velocity * 0.4));

      for (let i = 0; i < stars.length; i++) {
        const star = stars[i];
        star.twinklePhase += star.twinkleSpeed;
        star.alpha = star.baseAlpha + Math.sin(star.twinklePhase) * 0.15;
        if (star.alpha < 0.05) star.alpha = 0.05;

        const scrollOffsetY = scrollY * star.z * 0.25;
        const virtualY = (star.y - scrollOffsetY) % (height + 80);
        const drawY = virtualY < -40 ? virtualY + height + 80 : virtualY;
        const drawX = star.x + mouseParallaxX * star.z;

        ctx.fillStyle = star.color;
        ctx.globalAlpha = star.alpha;

        // Subtle stretch only on rapid scroll
        if (Math.abs(velocityStretch) > 1.5 && star.z > 0.7) {
          ctx.beginPath();
          ctx.moveTo(drawX, drawY);
          ctx.lineTo(drawX, drawY - velocityStretch * star.z);
          ctx.strokeStyle = star.color;
          ctx.lineWidth = star.size * 0.6;
          ctx.stroke();
        } else {
          ctx.beginPath();
          ctx.arc(drawX, drawY, star.size, 0, Math.PI * 2);
          ctx.fill();

          // Feature Star 4-Point Glint
          if (star.hasGlint && star.alpha > 0.4) {
            ctx.save();
            ctx.strokeStyle = star.color;
            ctx.lineWidth = 0.5;
            ctx.globalAlpha = star.alpha * 0.75;
            const glintSize = star.size * 3.5;
            ctx.beginPath();
            ctx.moveTo(drawX - glintSize, drawY);
            ctx.lineTo(drawX + glintSize, drawY);
            ctx.moveTo(drawX, drawY - glintSize);
            ctx.lineTo(drawX, drawY + glintSize);
            ctx.stroke();
            ctx.restore();
          }
        }
      }

      // 8. Supernova Interactive Bursts
      const bursts = burstsRef.current;
      for (let i = bursts.length - 1; i >= 0; i--) {
        const p = bursts[i];
        p.x += p.vx;
        p.y += p.vy;
        p.vx *= 0.95;
        p.vy *= 0.95;
        p.life++;
        p.alpha = 1 - p.life / p.maxLife;

        if (p.life >= p.maxLife) {
          bursts.splice(i, 1);
          continue;
        }

        ctx.fillStyle = p.color;
        ctx.globalAlpha = p.alpha;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size * (1 + p.life * 0.03), 0, Math.PI * 2);
        ctx.fill();
      }

      ctx.globalAlpha = 1;
      animationFrameId.current = requestAnimationFrame(render);
    };

    animationFrameId.current = requestAnimationFrame(render);

    return () => {
      if (animationFrameId.current) cancelAnimationFrame(animationFrameId.current);
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('mousemove', handleMouseMove);
    };
  }, []);

  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;

    if (onStarClick) onStarClick(1);
    soundEngine.playSupernovaBurst();

    const burstParticles: SupernovaParticle[] = [];
    const colors = ['#00F0FF', '#9D4EDD', '#FFFFFF', '#38BDF8'];

    for (let i = 0; i < 20; i++) {
      const angle = (Math.PI * 2 * i) / 20 + (Math.random() - 0.5) * 0.3;
      const speed = Math.random() * 3.5 + 1.2;
      burstParticles.push({
        x: clickX,
        y: clickY,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        size: Math.random() * 2 + 0.8,
        color: colors[Math.floor(Math.random() * colors.length)],
        alpha: 1,
        life: 0,
        maxLife: Math.floor(Math.random() * 20 + 25),
      });
    }

    burstsRef.current.push(...burstParticles);
  };

  return (
    <div className="fixed inset-0 pointer-events-none z-0 overflow-hidden select-none">
      <canvas
        ref={canvasRef}
        onClick={handleCanvasClick}
        className="w-full h-full block pointer-events-auto cursor-crosshair opacity-100"
      />
      {/* Subtle Cinematic Vignette */}
      <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(ellipse_at_center,transparent_55%,rgba(2,4,8,0.75)_100%)]" />
    </div>
  );
};
