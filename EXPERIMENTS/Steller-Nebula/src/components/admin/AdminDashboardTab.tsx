import React from 'react';
import { Layers, Plus, Settings, Download, Database, ShieldCheck, ArrowRight, ExternalLink } from 'lucide-react';
import { useContent } from '../../context/ContentContext';
import { soundEngine } from '../../utils/audioSystem';

interface AdminDashboardTabProps {
  onNavigate: (tab: 'projects' | 'settings' | 'backup') => void;
  onOpenAddProject: () => void;
  onEditProject: (id: string) => void;
}

export const AdminDashboardTab: React.FC<AdminDashboardTabProps> = ({
  onNavigate,
  onOpenAddProject,
  onEditProject
}) => {
  const { projects, studioConfig, isUsingCustomData } = useContent();

  return (
    <div className="space-y-8 animate-in fade-in duration-200">
      {/* Overview Cards */}
      <div className="grid sm:grid-cols-3 gap-4">
        {/* Projects Count Card */}
        <div className="p-5 rounded-xl border border-cyan-500/20 bg-slate-900/50 backdrop-blur space-y-2">
          <div className="flex items-center justify-between font-mono text-xs text-cyan-400">
            <span>PROJECTS CONFIGURED</span>
            <Layers className="w-4 h-4" />
          </div>
          <div className="text-3xl font-sans font-black text-white">
            {projects.length}
          </div>
          <div className="font-mono text-[11px] text-slate-400">
            {projects.length === 1 ? '1 project active' : `${projects.length} projects active`}
          </div>
        </div>

        {/* Site Status Card */}
        <div className="p-5 rounded-xl border border-white/10 bg-slate-900/50 backdrop-blur space-y-2">
          <div className="flex items-center justify-between font-mono text-xs text-slate-400">
            <span>SITE STATUS</span>
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
          </div>
          <div className="text-xl font-mono font-bold text-white tracking-wide">
            LOCAL EDITOR
          </div>
          <div className="font-mono text-[11px] text-slate-400">
            Realtime client-side rendering
          </div>
        </div>

        {/* Content Source Card */}
        <div className="p-5 rounded-xl border border-white/10 bg-slate-900/50 backdrop-blur space-y-2">
          <div className="flex items-center justify-between font-mono text-xs text-slate-400">
            <span>CONTENT SOURCE</span>
            <Database className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-xl font-mono font-bold text-cyan-300">
            {isUsingCustomData ? 'LOCAL OVERRIDE' : 'DEFAULT DATA'}
          </div>
          <div className="font-mono text-[11px] text-slate-400">
            {isUsingCustomData ? 'Saved in browser storage' : 'Initial studio config'}
          </div>
        </div>
      </div>

      {/* Local Storage Architecture Note */}
      <div className="p-4 rounded-xl border border-cyan-500/20 bg-cyan-950/20 flex items-start gap-3">
        <ShieldCheck className="w-5 h-5 text-cyan-400 shrink-0 mt-0.5" />
        <div className="space-y-1">
          <div className="font-mono text-xs font-bold text-cyan-300">
            FRONTEND CONTENT EDITOR
          </div>
          <p className="font-sans text-xs text-slate-300 leading-relaxed">
            Changes made in this panel are saved directly to this browser’s local storage. To migrate changes to another device or environment, use the <strong>Backup & Export</strong> feature.
          </p>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="space-y-3">
        <h3 className="font-mono text-xs text-slate-400 tracking-wider uppercase">
          QUICK ACTIONS
        </h3>
        <div className="grid sm:grid-cols-3 gap-3">
          <button
            onClick={() => {
              soundEngine.playButtonClick();
              onOpenAddProject();
            }}
            className="flex items-center justify-between p-4 rounded-xl border border-white/10 hover:border-cyan-400/50 bg-slate-900/60 hover:bg-cyan-950/30 text-left transition-all group"
          >
            <div className="space-y-1">
              <div className="font-sans font-bold text-sm text-white group-hover:text-cyan-300">
                + Add Project
              </div>
              <div className="font-mono text-[10px] text-slate-400">
                Create new verified project
              </div>
            </div>
            <Plus className="w-4 h-4 text-cyan-400 group-hover:scale-110 transition-transform" />
          </button>

          <button
            onClick={() => {
              soundEngine.playButtonClick();
              onNavigate('settings');
            }}
            className="flex items-center justify-between p-4 rounded-xl border border-white/10 hover:border-cyan-400/50 bg-slate-900/60 hover:bg-cyan-950/30 text-left transition-all group"
          >
            <div className="space-y-1">
              <div className="font-sans font-bold text-sm text-white group-hover:text-cyan-300">
                Site Settings
              </div>
              <div className="font-mono text-[10px] text-slate-400">
                Edit headlines & about text
              </div>
            </div>
            <Settings className="w-4 h-4 text-cyan-400 group-hover:rotate-45 transition-transform" />
          </button>

          <button
            onClick={() => {
              soundEngine.playButtonClick();
              onNavigate('backup');
            }}
            className="flex items-center justify-between p-4 rounded-xl border border-white/10 hover:border-cyan-400/50 bg-slate-900/60 hover:bg-cyan-950/30 text-left transition-all group"
          >
            <div className="space-y-1">
              <div className="font-sans font-bold text-sm text-white group-hover:text-cyan-300">
                Export / Backup
              </div>
              <div className="font-mono text-[10px] text-slate-400">
                Download JSON snapshot
              </div>
            </div>
            <Download className="w-4 h-4 text-cyan-400 group-hover:translate-y-0.5 transition-transform" />
          </button>
        </div>
      </div>

      {/* Current Real Projects List Preview */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="font-mono text-xs text-slate-400 tracking-wider uppercase">
            ACTIVE PROJECTS ({projects.length})
          </h3>
          <button
            onClick={() => {
              soundEngine.playButtonClick();
              onNavigate('projects');
            }}
            className="font-mono text-xs text-cyan-400 hover:text-cyan-300 inline-flex items-center gap-1"
          >
            <span>Manage All</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="space-y-2">
          {projects.map((project, idx) => (
            <div
              key={project.id}
              className="p-4 rounded-xl border border-white/10 bg-slate-900/40 flex items-center justify-between gap-4"
            >
              <div className="space-y-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs text-cyan-400 font-bold">
                    0{idx + 1}
                  </span>
                  <span className="font-sans font-bold text-white text-base truncate">
                    {project.name}
                  </span>
                  <span className="px-2 py-0.5 rounded bg-cyan-950/60 border border-cyan-500/30 text-cyan-300 font-mono text-[10px]">
                    {project.category}
                  </span>
                </div>
                <p className="font-sans text-xs text-slate-400 truncate max-w-xl">
                  {project.description}
                </p>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => {
                    soundEngine.playButtonClick();
                    onEditProject(project.id);
                  }}
                  className="px-3 py-1.5 rounded-lg border border-cyan-500/30 hover:border-cyan-400 bg-cyan-950/40 text-cyan-300 hover:text-white font-mono text-xs transition-colors"
                >
                  EDIT
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
