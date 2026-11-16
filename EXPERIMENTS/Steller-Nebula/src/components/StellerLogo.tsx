import React from 'react';

interface StellerLogoProps {
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'hero';
  className?: string;
  withGlow?: boolean;
  withRings?: boolean;
  interactive?: boolean;
  onClick?: () => void;
}

export const StellerLogo: React.FC<StellerLogoProps> = ({
  size = 'md',
  className = '',
  withGlow = true,
  withRings = false,
  interactive = false,
  onClick,
}) => {
  const sizeMap = {
    sm: 'w-6 h-6',
    md: 'w-8 h-8',
    lg: 'w-16 h-16',
    xl: 'w-24 h-24',
    hero: 'w-48 sm:w-60 md:w-72 h-48 sm:h-60 md:h-72',
  };

  const isHero = size === 'hero';

  return (
    <div
      onClick={onClick}
      className={`relative inline-flex items-center justify-center select-none ${sizeMap[size]} ${
        interactive ? 'cursor-pointer group' : ''
      } ${className}`}
    >
      {/* Optional Ambient Nebula Glow */}
      {withGlow && (
        <div
          className={`absolute rounded-full pointer-events-none transition-all duration-700 ${
            isHero
              ? 'w-[160%] h-[160%] blur-3xl opacity-65 group-hover:opacity-90'
              : 'w-[180%] h-[180%] blur-lg opacity-45 group-hover:opacity-75'
          }`}
          style={{
            background:
              'radial-gradient(circle, rgba(0,240,255,0.55) 0%, rgba(168,85,247,0.35) 40%, rgba(0,240,255,0.15) 60%, transparent 75%)',
          }}
        />
      )}

      {/* Optional Subtle Orbital Rings in Hero */}
      {withRings && (
        <>
          <svg
            className="absolute inset-[-40%] w-[180%] h-[180%] pointer-events-none opacity-45 group-hover:opacity-70 transition-opacity duration-500 animate-orbit-slow"
            viewBox="0 0 200 200"
            fill="none"
          >
            <ellipse
              cx="100"
              cy="100"
              rx="85"
              ry="42"
              transform="rotate(-25 100 100)"
              stroke="rgba(0, 240, 255, 0.6)"
              strokeWidth="1"
              strokeDasharray="2 4"
            />
          </svg>
          <svg
            className="absolute inset-[-40%] w-[180%] h-[180%] pointer-events-none opacity-35 group-hover:opacity-60 transition-opacity duration-500 animate-orbit-counter"
            viewBox="0 0 200 200"
            fill="none"
          >
            <ellipse
              cx="100"
              cy="100"
              rx="75"
              ry="32"
              transform="rotate(40 100 100)"
              stroke="rgba(192, 132, 252, 0.6)"
              strokeWidth="1"
            />
          </svg>
        </>
      )}

      {/* High-Precision SVG Crystal S + 4-Point Luminous Star Core */}
      <svg
        viewBox="0 0 120 140"
        className="w-full h-full relative z-10 transition-transform duration-500 group-hover:scale-105 filter drop-shadow-[0_0_22px_rgba(0,240,255,0.6)]"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          {/* Gradients for Cyan Top Arm - Vibrant & Bright */}
          <linearGradient id="cyanFacet1" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#E0F7FE" />
            <stop offset="35%" stopColor="#38BDF8" />
            <stop offset="100%" stopColor="#0284C7" />
          </linearGradient>

          <linearGradient id="cyanFacet2" x1="0%" y1="100%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#00F0FF" />
            <stop offset="50%" stopColor="#38BDF8" />
            <stop offset="100%" stopColor="#0369A1" />
          </linearGradient>

          <linearGradient id="cyanHighlight" x1="0%" y1="0%" x2="100%" y2="50%">
            <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0.95" />
            <stop offset="100%" stopColor="#7DD3FC" stopOpacity="0.4" />
          </linearGradient>

          {/* Gradients for Purple Bottom Arm - Vibrant Electric Purple */}
          <linearGradient id="purpleFacet1" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#67E8F9" />
            <stop offset="45%" stopColor="#C084FC" />
            <stop offset="100%" stopColor="#9333EA" />
          </linearGradient>

          <linearGradient id="purpleFacet2" x1="100%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#F3E8FF" />
            <stop offset="40%" stopColor="#C084FC" />
            <stop offset="100%" stopColor="#6B21A8" />
          </linearGradient>

          <linearGradient id="purpleHighlight" x1="100%" y1="100%" x2="0%" y2="0%">
            <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0.95" />
            <stop offset="100%" stopColor="#C084FC" stopOpacity="0.3" />
          </linearGradient>

          {/* Star Core Glow */}
          <radialGradient id="starGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#FFFFFF" />
            <stop offset="30%" stopColor="#00F0FF" stopOpacity="0.95" />
            <stop offset="65%" stopColor="#38BDF8" stopOpacity="0.6" />
            <stop offset="100%" stopColor="#00F0FF" stopOpacity="0" />
          </radialGradient>
        </defs>

        {/* --- TOP CRYSTALLINE ARM (Cyan/Electric Blue) --- */}
        <g className="transition-opacity duration-300">
          {/* Main Top Upper Spear Tip */}
          <polygon
            points="60,30 92,12 70,36 58,40"
            fill="url(#cyanFacet1)"
            stroke="rgba(255,255,255,0.7)"
            strokeWidth="0.6"
          />
          {/* Top Outer Curve Facet */}
          <polygon
            points="60,30 46,45 42,62 58,62 58,40"
            fill="url(#cyanFacet2)"
            stroke="rgba(0,240,255,0.8)"
            strokeWidth="0.6"
          />
          {/* Top Arch Facet */}
          <polygon
            points="60,30 70,36 58,40"
            fill="url(#cyanHighlight)"
          />
          {/* Top Front Triangular Facet */}
          <polygon
            points="46,45 58,40 54,55"
            fill="#BAE6FD"
            stroke="rgba(255,255,255,0.8)"
            strokeWidth="0.6"
          />
          <polygon
            points="58,40 54,55 58,62"
            fill="#0284C7"
          />
          {/* Top Inner Sharp Bevel (Pointing toward center star) */}
          <polygon
            points="42,62 54,55 58,72 44,70"
            fill="url(#cyanFacet1)"
            stroke="rgba(0,240,255,0.7)"
            strokeWidth="0.6"
          />
          <polygon
            points="44,70 58,72 58,78"
            fill="#38BDF8"
          />
        </g>

        {/* --- BOTTOM CRYSTALLINE ARM (Amethyst / Violet / Indigo) --- */}
        <g className="transition-opacity duration-300">
          {/* Bottom Lower Spear Tip */}
          <polygon
            points="60,110 28,128 50,104 62,100"
            fill="url(#purpleFacet2)"
            stroke="rgba(255,255,255,0.7)"
            strokeWidth="0.6"
          />
          {/* Bottom Outer Curve Facet */}
          <polygon
            points="60,110 74,95 78,78 62,78 62,100"
            fill="url(#purpleFacet1)"
            stroke="rgba(192,132,252,0.8)"
            strokeWidth="0.6"
          />
          {/* Bottom Arch Facet */}
          <polygon
            points="60,110 50,104 62,100"
            fill="url(#purpleHighlight)"
          />
          {/* Bottom Front Triangular Facet */}
          <polygon
            points="74,95 62,100 66,85"
            fill="#E9D5FF"
            stroke="rgba(255,255,255,0.8)"
            strokeWidth="0.6"
          />
          <polygon
            points="62,100 66,85 62,78"
            fill="#7E22CE"
          />
          {/* Bottom Inner Sharp Bevel (Pointing toward center star) */}
          <polygon
            points="78,78 66,85 62,68 76,70"
            fill="url(#purpleFacet2)"
            stroke="rgba(192,132,252,0.7)"
            strokeWidth="0.6"
          />
          <polygon
            points="76,70 62,68 62,62"
            fill="#A855F7"
          />
        </g>

        {/* --- CENTRAL 4-POINT LUMINOUS STAR FLARE --- */}
        <g className="filter drop-shadow-[0_0_12px_#FFFFFF] drop-shadow-[0_0_20px_#00F0FF]">
          {/* Radial Aura Glow */}
          <circle cx="60" cy="70" r="16" fill="url(#starGlow)" opacity="0.95" />

          {/* Vertical Flare Ray */}
          <polygon
            points="60,42 63,70 60,98 57,70"
            fill="#FFFFFF"
            opacity="1"
          />
          {/* Horizontal Flare Ray */}
          <polygon
            points="32,70 60,73 88,70 60,67"
            fill="#FFFFFF"
            opacity="1"
          />

          {/* Micro 45-degree Cross Glow */}
          <polygon
            points="50,60 60,70 70,80 60,70"
            stroke="rgba(0,240,255,0.9)"
            strokeWidth="0.8"
          />
          <polygon
            points="70,60 60,70 50,80 60,70"
            stroke="rgba(192,132,252,0.9)"
            strokeWidth="0.8"
          />

          {/* Central Bright Star Diamond Core */}
          <polygon
            points="60,64 66,70 60,76 54,70"
            fill="#FFFFFF"
            filter="drop-shadow(0 0 6px #00F0FF)"
          />
        </g>
      </svg>
    </div>
  );
};
