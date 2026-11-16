import React from 'react';
import { ExternalLink, Sparkles, Code2, Compass, Layers, ArrowUpRight } from 'lucide-react';
import { useContent } from '../context/ContentContext';
import { useScrollJourney } from '../context/ScrollJourneyContext';
import { soundEngine } from '../utils/audioSystem';

export const AboutSection: React.FC = () => {
  const { studioConfig, projects } = useContent();
  const { sectionProgress } = useScrollJourney();

  const p = sectionProgress.about || 0;

  // Active / Built projects from the real project data
  const builtProjects = projects.filter(
    (proj) => !proj.status?.toLowerCase().includes('development')
  );

  // Projects marked as In Development from the real project data
  const inDevelopmentProjects = projects.filter((proj) =>
    proj.status?.toLowerCase().includes('development')
  );

  return (
    <section
      id="about"
      className="relative py-28 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto select-none overflow-hidden"
    >
      {/* Section Header */}
      <div className="relative z-10 space-y-2 mb-14 border-b border-white/10 pb-6">
        <div className="flex items-center gap-2 font-mono text-[11px] text-purple-400">
          <span className="w-1.5 h-1.5 rounded-full bg-purple-400 shadow-[0_0_8px_#A855F7]" />
          <span className="tracking-widest uppercase font-semibold">ABOUT</span>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
          <div>
            <h2 className="text-3xl sm:text-4xl font-sans font-bold tracking-tight text-white uppercase drop-shadow-[0_0_25px_rgba(255,255,255,0.35)]">
              ABOUT STELLER-NEBULA
            </h2>
            <p className="text-base sm:text-lg text-cyan-300 font-sans mt-1 drop-shadow-[0_0_12px_rgba(0,240,255,0.4)]">
              {studioConfig.aboutIntro || "A place for the things I'm building."}
            </p>
          </div>

          <div className="font-mono text-xs text-slate-300 bg-[#050811]/80 border border-purple-500/30 px-3 py-1.5 rounded-md flex items-center gap-2 self-start sm:self-auto">
            <span>EST. {studioConfig.year}</span>
          </div>
        </div>
      </div>

      {/* 1. Core Introduction Card */}
      <div
        className="relative z-10 rounded-xl border border-white/15 hover:border-purple-500/40 bg-[#050811]/85 backdrop-blur-xl p-8 sm:p-12 mb-12 shadow-[0_0_40px_rgba(0,0,0,0.5)] transition-all duration-200"
        style={{
          transform: `translate3d(0, ${(0.5 - p) * 16}px, 0)`,
        }}
      >
        <div className="max-w-3xl space-y-6">
          <div className="font-mono text-[11px] text-purple-300 tracking-wider uppercase font-semibold flex items-center gap-2">
            <Sparkles className="w-3.5 h-3.5 text-purple-400" />
            <span>WHAT IS STELLER-NEBULA</span>
          </div>

          <h3 className="text-2xl sm:text-3xl font-sans font-bold text-white leading-snug drop-shadow-[0_0_20px_rgba(255,255,255,0.25)]">
            An independent parent brand for projects, tools, and software.
          </h3>

          <p className="text-base sm:text-lg text-slate-100 font-sans leading-relaxed">
            {studioConfig.aboutText}
          </p>
        </div>
      </div>

      {/* 2. What I've Built (Real Projects Source of Truth) */}
      <div className="relative z-10 space-y-6 mb-12">
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <div className="flex items-center gap-2">
            <Layers className="w-4 h-4 text-cyan-400" />
            <h3 className="font-sans font-bold text-lg text-white uppercase tracking-wider">
              WHAT I'VE BUILT
            </h3>
          </div>
          <span className="font-mono text-xs text-slate-400">
            {builtProjects.length} {builtProjects.length === 1 ? 'project' : 'projects'} confirmed
          </span>
        </div>

        <div className="grid gap-6">
          {builtProjects.map((project) => (
            <div
              key={project.id}
              className="rounded-xl border border-white/15 hover:border-cyan-500/40 bg-[#050C16]/80 backdrop-blur-xl p-6 sm:p-8 transition-all duration-300 group"
            >
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                <div className="space-y-3 max-w-2xl">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-[11px] px-2.5 py-0.5 rounded bg-cyan-950/60 border border-cyan-500/40 text-cyan-200 font-semibold tracking-wider uppercase">
                      {project.category}
                    </span>
                    {project.status && (
                      <span className="font-mono text-[10px] text-slate-300 px-2 py-0.5 rounded bg-white/5 border border-white/10">
                        {project.status}
                      </span>
                    )}
                    {project.year && (
                      <span className="font-mono text-[10px] text-slate-400">
                        {project.year}
                      </span>
                    )}
                  </div>

                  <div>
                    <h4 className="text-xl sm:text-2xl font-sans font-bold text-white group-hover:text-cyan-300 transition-colors">
                      {project.name}
                    </h4>
                    {project.tagline && (
                      <p className="font-sans font-medium text-sm text-cyan-400/90 mt-0.5">
                        "{project.tagline}"
                      </p>
                    )}
                  </div>

                  <p className="font-sans text-sm sm:text-base text-slate-200 leading-relaxed">
                    {project.description}
                  </p>

                  {/* Highlights / Features */}
                  {project.features && project.features.length > 0 && (
                    <div className="flex flex-wrap gap-2 pt-2">
                      {project.features.map((feature, fIdx) => (
                        <span
                          key={fIdx}
                          className="font-sans text-xs px-2.5 py-1 rounded bg-[#030509]/80 border border-white/10 text-slate-300"
                        >
                          {feature}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                {project.url && (
                  <div className="shrink-0 pt-2 sm:pt-0">
                    <a
                      href={project.url}
                      target="_blank"
                      rel="noreferrer"
                      onClick={() => soundEngine.playButtonClick()}
                      onMouseEnter={() => soundEngine.playHoverBlip(880)}
                      className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg border border-cyan-500/40 hover:border-cyan-300 bg-cyan-950/40 hover:bg-cyan-900/60 text-cyan-200 hover:text-white font-mono text-xs uppercase tracking-wider transition-all duration-200 shadow-[0_0_15px_rgba(0,240,255,0.15)]"
                    >
                      <span>VISIT PROJECT</span>
                      <ExternalLink className="w-3.5 h-3.5" />
                    </a>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 3. Currently Building & What's Next Grid */}
      <div className="grid md:grid-cols-2 gap-6 relative z-10 mb-12">
        {/* Currently Building */}
        <div className="rounded-xl border border-white/15 bg-[#050811]/80 backdrop-blur-xl p-6 sm:p-8 space-y-4 flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-mono text-xs text-amber-300 font-semibold uppercase tracking-wider">
                CURRENTLY BUILDING
              </span>
              <span className="w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
            </div>

            {inDevelopmentProjects.length > 0 ? (
              <div className="space-y-3 pt-2">
                {inDevelopmentProjects.map((devProj) => (
                  <div key={devProj.id} className="p-3 rounded-lg bg-black/40 border border-white/10 space-y-1">
                    <div className="font-sans font-bold text-white text-sm">
                      {devProj.name}
                    </div>
                    <p className="font-sans text-xs text-slate-300">
                      {devProj.description}
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="font-sans text-sm sm:text-base text-slate-200 leading-relaxed pt-1">
                More projects are being worked on and will appear here when they're ready.
              </p>
            )}
          </div>

          <div className="font-mono text-[10px] text-slate-400 pt-3 border-t border-white/10">
            Real in-development work only.
          </div>
        </div>

        {/* Why It Exists */}
        <div className="rounded-xl border border-white/15 bg-[#050811]/80 backdrop-blur-xl p-6 sm:p-8 space-y-4 flex flex-col justify-between">
          <div className="space-y-3">
            <span className="font-mono text-xs text-purple-300 font-semibold uppercase tracking-wider">
              WHY IT EXISTS
            </span>

            <p className="font-sans text-sm sm:text-base text-slate-200 leading-relaxed pt-1">
              {studioConfig.whyItExists}
            </p>
          </div>

          <div className="font-mono text-[10px] text-slate-400 pt-3 border-t border-white/10">
            One home instead of scattered projects.
          </div>
        </div>
      </div>

      {/* 4. The Developer Section */}
      <div className="relative z-10 rounded-xl border border-white/15 bg-[#050811]/85 backdrop-blur-xl p-8 sm:p-10 mb-12 shadow-lg">
        <div className="flex flex-col md:flex-row md:items-start justify-between gap-8">
          <div className="space-y-4 max-w-2xl">
            <div className="font-mono text-xs text-teal-300 font-semibold uppercase tracking-wider flex items-center gap-2">
              <Code2 className="w-4 h-4 text-teal-400" />
              <span>THE DEVELOPER</span>
            </div>

            {studioConfig.developer?.name && (
              <h3 className="text-2xl sm:text-3xl font-sans font-bold text-white tracking-tight">
                {studioConfig.developer.name}
              </h3>
            )}

            <p className="font-sans text-sm sm:text-base text-slate-200 leading-relaxed">
              {studioConfig.developer?.bio ||
                "I'm a developer who likes turning ideas into things people can actually use. Steller-Nebula is where I collect those projects — from websites and software to creative experiments."}
            </p>

            {/* Genuine Interests list if available */}
            {studioConfig.developer?.interests && studioConfig.developer.interests.length > 0 && (
              <div className="pt-2">
                <div className="font-mono text-[10px] text-slate-400 uppercase tracking-wider mb-2">
                  FOCUS & EXPLORATIONS
                </div>
                <div className="flex flex-wrap gap-2">
                  {studioConfig.developer.interests.map((interest, idx) => (
                    <span
                      key={idx}
                      className="font-sans text-xs px-2.5 py-1 rounded bg-[#030509]/80 border border-teal-500/20 text-slate-300"
                    >
                      {interest}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Optional Profile Picture (Only if valid URL is explicitly configured) */}
          {studioConfig.developer?.avatarUrl ? (
            <div className="shrink-0">
              <img
                src={studioConfig.developer.avatarUrl}
                alt={studioConfig.developer.name || 'Developer'}
                className="w-24 h-24 sm:w-28 sm:h-28 rounded-xl object-cover border border-teal-500/40 shadow-[0_0_20px_rgba(45,212,191,0.2)]"
              />
            </div>
          ) : null}
        </div>
      </div>

      {/* 5. How I Build (Simple Philosophy Statements, No Fake Metrics) */}
      <div className="relative z-10 space-y-6 mb-12">
        <div className="flex items-center gap-2 border-b border-white/10 pb-3">
          <Compass className="w-4 h-4 text-purple-400" />
          <h3 className="font-sans font-bold text-lg text-white uppercase tracking-wider">
            HOW I BUILD
          </h3>
        </div>

        <div className="grid md:grid-cols-3 gap-5">
          {studioConfig.howIBuild?.map((item, idx) => (
            <div
              key={idx}
              className="p-6 rounded-xl border border-white/15 bg-[#050811]/80 hover:border-purple-400/50 transition-all duration-300 space-y-2.5 group shadow-lg"
            >
              <h4 className="font-sans font-bold text-base text-white group-hover:text-purple-200 transition-colors">
                {item.title}
              </h4>

              <p className="font-sans text-xs sm:text-sm text-slate-300 leading-relaxed">
                {item.description}
              </p>
            </div>
          ))}
        </div>
      </div>

      {/* 6. What's Next & Timeline Summary */}
      <div className="relative z-10 rounded-xl border border-white/15 bg-[#050811]/70 backdrop-blur-md p-6 sm:p-8 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
          <div>
            <div className="font-mono text-[11px] text-cyan-300 font-semibold uppercase tracking-wider">
              WHAT'S NEXT
            </div>
            <p className="font-sans text-sm sm:text-base text-slate-200 mt-1 max-w-2xl leading-relaxed">
              {studioConfig.whatsNext}
            </p>
          </div>

          <div className="font-mono text-xs text-slate-400">
            {studioConfig.year}
          </div>
        </div>

        {/* Small Real Timeline */}
        <div className="pt-2">
          <div className="font-mono text-[10px] text-slate-400 uppercase tracking-wider mb-3">
            RECORDED TIMELINE
          </div>
          <div className="grid sm:grid-cols-2 gap-3 font-mono text-xs">
            <div className="p-3 rounded bg-slate-900/60 border border-white/5 space-y-1">
              <span className="text-cyan-400 font-bold">{studioConfig.year}</span>
              <div className="font-sans font-bold text-white">Steller-Nebula</div>
              <p className="font-sans text-xs text-slate-300">
                Created as the parent home for independent projects.
              </p>
            </div>

            {builtProjects.map((p) => (
              <div key={p.id} className="p-3 rounded bg-slate-900/60 border border-white/5 space-y-1">
                <span className="text-cyan-400 font-bold">{p.year || studioConfig.year}</span>
                <div className="font-sans font-bold text-white">{p.name}</div>
                <p className="font-sans text-xs text-slate-300">
                  {p.category} — {p.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};
