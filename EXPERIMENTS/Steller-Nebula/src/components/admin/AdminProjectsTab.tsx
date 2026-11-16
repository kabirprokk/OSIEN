import React, { useState } from 'react';
import { Plus, ArrowUp, ArrowDown, Edit3, Trash2, ExternalLink, AlertTriangle, X } from 'lucide-react';
import { ProjectItem } from '../../types';
import { useContent } from '../../context/ContentContext';
import { soundEngine } from '../../utils/audioSystem';

interface AdminProjectsTabProps {
  onOpenAdd: () => void;
  onEdit: (project: ProjectItem) => void;
  onShowToast: (message: string) => void;
}

export const AdminProjectsTab: React.FC<AdminProjectsTabProps> = ({
  onOpenAdd,
  onEdit,
  onShowToast
}) => {
  const { projects, deleteProject, moveProject } = useContent();
  const [projectToDelete, setProjectToDelete] = useState<ProjectItem | null>(null);

  const handleDeleteConfirm = () => {
    if (projectToDelete) {
      soundEngine.playButtonClick();
      deleteProject(projectToDelete.id);
      onShowToast(`PROJECT "${projectToDelete.name}" DELETED`);
      setProjectToDelete(null);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Header & Add Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
        <div>
          <h3 className="font-sans font-bold text-lg text-white">
            MANAGED PROJECTS ({projects.length})
          </h3>
          <p className="font-sans text-xs text-slate-400">
            Real projects displayed in the Steller-Nebula portfolio matrix.
          </p>
        </div>

        <button
          onClick={() => {
            soundEngine.playButtonClick();
            onOpenAdd();
          }}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-mono text-xs font-bold tracking-wider uppercase transition-all shadow-[0_0_15px_rgba(0,240,255,0.25)] self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>ADD PROJECT</span>
        </button>
      </div>

      {/* Projects List */}
      {projects.length === 0 ? (
        <div className="text-center py-12 border border-white/10 rounded-xl bg-slate-900/30 space-y-3">
          <p className="font-mono text-sm text-slate-400">No projects currently configured.</p>
          <button
            onClick={onOpenAdd}
            className="font-mono text-xs text-cyan-400 hover:underline"
          >
            Click here to add your first project
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {projects.map((project, idx) => (
            <div
              key={project.id}
              className="p-5 rounded-xl border border-white/10 bg-slate-900/50 hover:border-white/20 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
            >
              {/* Info Column */}
              <div className="space-y-1.5 min-w-0 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-mono text-xs text-cyan-400 font-bold">
                    0{idx + 1}
                  </span>
                  <span className="font-sans font-bold text-base text-white">
                    {project.name}
                  </span>
                  <span className="px-2 py-0.5 rounded bg-cyan-950/60 border border-cyan-500/30 text-cyan-300 font-mono text-[10px]">
                    {project.category}
                  </span>
                  <span className="px-2 py-0.5 rounded bg-slate-800 border border-white/10 text-slate-300 font-mono text-[10px]">
                    {project.status || 'Active'}
                  </span>
                </div>

                {project.tagline && (
                  <div className="font-serif italic text-xs text-cyan-200/80">
                    "{project.tagline}"
                  </div>
                )}

                <p className="font-sans text-xs text-slate-400 line-clamp-2 leading-relaxed">
                  {project.description}
                </p>

                {project.url && (
                  <div className="pt-1">
                    <a
                      href={project.url}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1 font-mono text-[11px] text-cyan-400 hover:text-cyan-300"
                    >
                      <span className="truncate max-w-xs">{project.url}</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                )}
              </div>

              {/* Actions Column */}
              <div className="flex items-center gap-2 shrink-0 self-end md:self-center pt-2 md:pt-0 border-t md:border-t-0 border-white/5 w-full md:w-auto justify-end">
                {/* Reorder Buttons */}
                <div className="flex items-center border border-white/10 rounded-lg overflow-hidden bg-slate-950">
                  <button
                    onClick={() => {
                      soundEngine.playButtonClick();
                      moveProject(idx, 'up');
                      onShowToast('ORDER UPDATED');
                    }}
                    disabled={idx === 0}
                    className="p-2 text-slate-400 hover:text-white disabled:opacity-30 disabled:hover:text-slate-400 transition-colors"
                    title="Move up in portfolio"
                  >
                    <ArrowUp className="w-3.5 h-3.5" />
                  </button>
                  <div className="w-[1px] h-4 bg-white/10" />
                  <button
                    onClick={() => {
                      soundEngine.playButtonClick();
                      moveProject(idx, 'down');
                      onShowToast('ORDER UPDATED');
                    }}
                    disabled={idx === projects.length - 1}
                    className="p-2 text-slate-400 hover:text-white disabled:opacity-30 disabled:hover:text-slate-400 transition-colors"
                    title="Move down in portfolio"
                  >
                    <ArrowDown className="w-3.5 h-3.5" />
                  </button>
                </div>

                {/* Edit Button */}
                <button
                  onClick={() => {
                    soundEngine.playButtonClick();
                    onEdit(project);
                  }}
                  className="px-3 py-2 rounded-lg border border-cyan-500/30 hover:border-cyan-400 bg-cyan-950/40 text-cyan-300 hover:text-white font-mono text-xs inline-flex items-center gap-1.5 transition-colors"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                  <span>EDIT</span>
                </button>

                {/* Delete Button */}
                <button
                  onClick={() => {
                    soundEngine.playButtonClick();
                    setProjectToDelete(project);
                  }}
                  className="p-2 rounded-lg border border-rose-500/30 hover:border-rose-400 bg-rose-950/30 text-rose-300 hover:text-rose-100 transition-colors"
                  title="Delete project"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {projectToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="relative w-full max-w-md p-6 rounded-2xl border border-rose-500/40 bg-[#080B12] text-slate-100 shadow-2xl space-y-5">
            <div className="flex items-start gap-3">
              <div className="p-2 rounded-lg bg-rose-950/60 border border-rose-500/30 text-rose-400">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div className="space-y-1">
                <h4 className="font-sans font-bold text-base text-white">
                  DELETE PROJECT?
                </h4>
                <p className="font-sans text-xs text-slate-400 leading-relaxed">
                  Are you sure you want to remove <strong>"{projectToDelete.name}"</strong>? This will remove it from the Steller-Nebula portfolio matrix on this browser.
                </p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setProjectToDelete(null)}
                className="px-4 py-2 rounded-lg border border-white/10 text-slate-300 hover:text-white font-mono text-xs transition-colors"
              >
                CANCEL
              </button>
              <button
                onClick={handleDeleteConfirm}
                className="px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-mono text-xs font-bold uppercase transition-colors shadow-lg shadow-rose-950"
              >
                DELETE PROJECT
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
