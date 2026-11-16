import React, { useState } from 'react';
import {
  ArrowUpRight,
  Sparkles,
  BookOpen,
  PanelsTopLeft,
} from 'lucide-react';
import { useContent } from '../context/ContentContext';
import { useScrollJourney } from '../context/ScrollJourneyContext';
import { soundEngine } from '../utils/audioSystem';

export const ProjectsUniverse: React.FC = () => {
  const { projects } = useContent();
  const { sectionProgress } = useScrollJourney();

  const [tilts, setTilts] = useState<Record<string, { x: number; y: number }>>({});
  const [hoveredProject, setHoveredProject] = useState<string | null>(null);

  const p = sectionProgress.projects || 0;

  const encounterFocus = Math.sin(
    Math.max(0, Math.min(Math.PI, p * Math.PI))
  );

  const scale = 0.92 + encounterFocus * 0.08;
  const projectTranslateY = (0.5 - p) * 40;

  /*
   * Only the first two real projects are shown as featured.
   * More projects can be added to PROJECTS_DATA without changing
   * the component.
   */
 const featuredProjects = projects.filter(
  (project) =>
    project.id === 'storyteller' ||
    project.id === 'keepthestyle'
);

  const handleProjectMouseMove = (
    e: React.MouseEvent<HTMLDivElement>,
    projectId: string
  ) => {
    const rect = e.currentTarget.getBoundingClientRect();

    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;

    setTilts((prev) => ({
      ...prev,
      [projectId]: {
        x: x * 16,
        y: -y * 16,
      },
    }));
  };

  const handleProjectMouseLeave = (projectId: string) => {
    setHoveredProject(null);

    setTilts((prev) => ({
      ...prev,
      [projectId]: {
        x: 0,
        y: 0,
      },
    }));
  };

  const getProjectIcon = (projectId: string) => {
    if (projectId === 'keepthestyle') {
      return PanelsTopLeft;
    }

    return BookOpen;
  };

  const getProjectAction = (projectName: string) => {
    return `EXPLORE ${projectName.toUpperCase()}`;
  };

  return (
    <section
      id="projects"
      className="relative py-28 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto select-none overflow-hidden"
    >
      {/* Section Header */}
      <div className="relative z-10 space-y-2 mb-14 border-b border-white/5 pb-6">
        <div className="flex items-center gap-2 font-mono text-[11px] text-cyan-400">
          <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 shadow-[0_0_6px_#00F0FF]" />
          <span className="tracking-widest uppercase">
            FEATURED WORK
          </span>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
          <div>
            <h2 className="text-3xl sm:text-4xl font-sans font-bold tracking-tight text-white uppercase drop-shadow-[0_0_20px_rgba(255,255,255,0.3)]">
              PROJECTS
            </h2>

            <p className="text-sm sm:text-base text-slate-300 font-sans mt-1">
              Projects I&apos;ve built under Steller-Nebula.
            </p>
          </div>

          <div className="font-mono text-xs text-slate-300 bg-[#050C16]/80 border border-cyan-500/30 px-3 py-1.5 rounded-md flex items-center gap-2 self-start sm:self-auto">
            <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 shadow-[0_0_6px_#00F0FF]" />
            <span>FEATURED</span>
          </div>
        </div>
      </div>

      {/* Featured Projects */}
      <div
        className="relative will-change-transform transition-all duration-150 ease-out space-y-10"
        style={{
          transform: `translate3d(0, ${projectTranslateY}px, 0) scale(${scale})`,
        }}
      >
        {featuredProjects.map((project, projectIndex) => {
          const Icon = getProjectIcon(project.id);

          const tilt = tilts[project.id] || { x: 0, y: 0 };
          const isHovering = hoveredProject === project.id;

          return (
            <article
              key={project.id}
              className="relative"
            >
              {/* Subtle Aura Glow */}
              <div
                className="absolute -inset-1 rounded-2xl blur-2xl transition-opacity duration-500 pointer-events-none"
                style={{
                  background:
                    'radial-gradient(ellipse at center, rgba(0,240,255,0.18) 0%, rgba(124,58,237,0.08) 50%, transparent 80%)',
                  opacity: Math.max(
                    0.25,
                    encounterFocus * (projectIndex === 0 ? 1 : 0.8)
                  ),
                }}
              />

              {/* Main Glass Container */}
              <div className="relative rounded-xl border border-white/15 hover:border-cyan-500/40 bg-[#050811]/85 backdrop-blur-xl p-8 sm:p-12 shadow-[0_0_50px_rgba(0,0,0,0.6)] transition-colors duration-300">
                <div className="grid lg:grid-cols-12 gap-10 lg:gap-12 items-center">
                  {/* Project Narrative */}
                  <div className="lg:col-span-7 space-y-5">
                    {/* Category + Status */}
                    <div className="flex flex-wrap items-center gap-3">
                      <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-cyan-950/60 border border-cyan-500/40 text-cyan-200 font-mono text-[11px] font-semibold">
                        <Sparkles className="w-3 h-3 text-cyan-400" />

                        <span className="tracking-wider">
                          {project.category}
                        </span>
                      </div>

                      <div className="font-mono text-[10px] text-slate-300 px-2 py-0.5 rounded bg-white/10 border border-white/15">
                        {project.status || 'ACTIVE'}
                      </div>
                    </div>

                    {/* Name + Tagline */}
                    <div>
                      <h3 className="text-3xl sm:text-4xl font-sans font-bold text-white tracking-tight drop-shadow-[0_0_20px_rgba(255,255,255,0.25)]">
                        {project.name}
                      </h3>

                      {project.tagline && (
                        <p className="font-sans font-medium text-base sm:text-lg text-cyan-300 mt-1">
                          &quot;{project.tagline}&quot;
                        </p>
                      )}
                    </div>

                    {/* Description */}
                    <p className="text-base text-slate-100 font-sans leading-relaxed">
                      {project.description}
                    </p>

                    {/* Features */}
                    {project.features &&
                      project.features.length > 0 && (
                        <div className="space-y-2 pt-2 border-t border-white/10">
                          <span className="font-mono text-[10px] text-slate-300 tracking-widest block uppercase font-semibold">
                            KEY FEATURES
                          </span>

                          <div className="grid sm:grid-cols-2 gap-2 font-sans text-xs text-slate-100">
                            {project.features.map((feature, index) => (
                              <div
                                key={`${project.id}-feature-${index}`}
                                className="flex items-center gap-2 p-2 rounded bg-[#030509]/80 border border-white/10"
                              >
                                <div className="w-1 h-1 rounded-full bg-cyan-400 shadow-[0_0_6px_#00F0FF]" />

                                <span>{feature}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                    {/* Action */}
                    <div className="pt-3">
                      {project.url && (
                        <a
                          href={project.url}
                          target="_blank"
                          rel="noreferrer"
                          onClick={() =>
                            soundEngine.playButtonClick()
                          }
                          onMouseEnter={() =>
                            soundEngine.playHoverBlip(980)
                          }
                          className="inline-flex items-center gap-2 px-6 py-3 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-mono text-xs font-bold tracking-wider uppercase transition-all duration-200 shadow-[0_0_20px_rgba(0,240,255,0.3)] hover:shadow-[0_0_30px_rgba(0,240,255,0.5)] cursor-pointer"
                        >
                          <span>
                            {getProjectAction(project.name)}
                          </span>

                          <ArrowUpRight className="w-4 h-4" />
                        </a>
                      )}
                    </div>
                  </div>

                  {/* Right: Interactive Project Artifact */}
                  <div
                    onMouseMove={(e) =>
                      handleProjectMouseMove(e, project.id)
                    }
                    onMouseEnter={() => {
                      setHoveredProject(project.id);
                    }}
                    onMouseLeave={() =>
                      handleProjectMouseLeave(project.id)
                    }
                    className="lg:col-span-5 flex items-center justify-center p-2 cursor-pointer"
                  >
                    <div
                      className="relative w-56 sm:w-64 h-72 sm:h-80 rounded-xl transition-transform duration-200 ease-out"
                      style={{
                        transform: `
                          perspective(1000px)
                          rotateY(${tilt.x}deg)
                          rotateX(${tilt.y}deg)
                          scale(${isHovering ? 1.03 : 1})
                        `,
                        transformStyle: 'preserve-3d',
                      }}
                    >
                      {/* Glow */}
                      <div className="absolute -inset-4 rounded-xl bg-cyan-400/15 blur-2xl opacity-60 pointer-events-none" />

                      {/* Holographic Project Card */}
                      <div className="relative w-full h-full rounded-lg bg-gradient-to-br from-[#081220] via-[#050811] to-[#020408] border border-cyan-400/40 p-6 flex flex-col justify-between shadow-[0_20px_40px_rgba(0,0,0,0.8)] overflow-hidden">
                        {/* Side Accent */}
                        <div className="absolute top-0 left-0 bottom-0 w-1 bg-gradient-to-b from-cyan-400 to-purple-500 shadow-[0_0_8px_#00F0FF]" />

                        {/* Top Metadata */}
                        <div className="relative z-10 flex items-center justify-between font-mono text-[9px] text-cyan-300">
                          <span className="tracking-widest font-bold">
                            {project.name.toUpperCase()}
                          </span>

                          <span>
                            {project.year || '2026'}
                          </span>
                        </div>

                        {/* Center Icon + Title */}
                        <div className="relative z-10 text-center space-y-3 py-4">
                          <div className="w-12 h-12 rounded-full border border-cyan-400/40 bg-cyan-950/50 mx-auto flex items-center justify-center text-cyan-300 shadow-[0_0_15px_rgba(0,240,255,0.3)]">
                            <Icon className="w-6 h-6 animate-pulse" />
                          </div>

                          <div className="font-sans font-bold text-lg text-white tracking-wide truncate px-2">
                            {project.name}
                          </div>

                          <div className="w-8 h-[1px] bg-cyan-400/40 mx-auto" />

                          <div className="font-mono text-[9px] text-slate-400 uppercase tracking-wider px-2">
                            {project.category}
                          </div>
                        </div>

                        {/* Bottom Metadata */}
                        <div className="relative z-10 font-mono text-[9px] text-slate-400 flex items-center justify-between pt-3 border-t border-white/5">
                          <span className="text-cyan-300">
                            {project.status || 'ACTIVE'}
                          </span>

                          <span className="text-slate-400">
                            {project.year || '2026'}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </article>
          );
        })}
      </div>

      {/* Growth State */}
      <div className="mt-12 text-center py-6 border border-white/5 rounded-lg bg-[#050811]/40 relative z-10">
        <span className="font-mono text-[11px] text-slate-300 tracking-widest block uppercase">
          MORE PROJECTS IN DEVELOPMENT
        </span>

        <p className="font-sans text-xs text-slate-400 mt-1">
          New software, tools, and creative experiments will be added here as they are released.
        </p>
      </div>
    </section>
  );
};
