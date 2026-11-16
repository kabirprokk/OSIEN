import React, { useState, useEffect } from 'react';
import {
  LayoutDashboard,
  Layers,
  Settings,
  Database,
  Lock,
  LogOut,
  X,
  Sparkles,
  ShieldCheck,
  CheckCircle2
} from 'lucide-react';
import { AdminLockScreen } from './AdminLockScreen';
import { AdminDashboardTab } from './AdminDashboardTab';
import { AdminProjectsTab } from './AdminProjectsTab';
import { AdminSiteSettingsTab } from './AdminSiteSettingsTab';
import { AdminBackupTab } from './AdminBackupTab';
import { AdminProjectModal } from './AdminProjectModal';
import { ProjectItem } from '../../types';
import { useContent } from '../../context/ContentContext';
import { soundEngine } from '../../utils/audioSystem';

interface AdminPanelProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AdminPanel: React.FC<AdminPanelProps> = ({ isOpen, onClose }) => {
  const { projects } = useContent();
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [activeTab, setActiveTab] = useState<'dashboard' | 'projects' | 'settings' | 'backup'>('dashboard');

  // Project Add / Edit Modal state
  const [isProjectModalOpen, setIsProjectModalOpen] = useState(false);
  const [projectToEdit, setProjectToEdit] = useState<ProjectItem | null>(null);

  // Toast feedback state
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    // Listen for ESC key to close if open
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isProjectModalOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isProjectModalOpen, onClose]);

  const showToast = (message: string) => {
    setToastMessage(message);
    setTimeout(() => {
      setToastMessage(null);
    }, 3000);
  };

  const handleOpenAddProject = () => {
    setProjectToEdit(null);
    setIsProjectModalOpen(true);
  };

  const handleEditProject = (project: ProjectItem) => {
    setProjectToEdit(project);
    setIsProjectModalOpen(true);
  };

  const handleEditProjectById = (id: string) => {
    const p = projects.find((item) => item.id === id);
    if (p) {
      setProjectToEdit(p);
      setIsProjectModalOpen(true);
    }
  };

  const handleLockSession = () => {
    soundEngine.playButtonClick();
    setIsAuthenticated(false);
    showToast('SESSION LOCKED');
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 md:p-6 bg-black/85 backdrop-blur-xl animate-in fade-in duration-200">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed top-6 right-6 z-50 flex items-center gap-2 px-4 py-2.5 rounded-lg bg-cyan-950/95 border border-cyan-400/50 text-cyan-200 font-mono text-xs shadow-[0_0_30px_rgba(0,240,255,0.4)] animate-in slide-in-from-top-2 duration-150">
          <CheckCircle2 className="w-4 h-4 text-cyan-400" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Lock Screen if not authenticated */}
      {!isAuthenticated ? (
        <AdminLockScreen
          onUnlock={() => setIsAuthenticated(true)}
          onClose={onClose}
        />
      ) : (
        /* Full Admin Panel Container */
        <div className="relative w-full max-w-5xl h-[92vh] max-h-[850px] flex flex-col rounded-2xl border border-cyan-500/30 bg-[#080B12] text-slate-100 shadow-[0_0_100px_rgba(0,240,255,0.15)] overflow-hidden">
          {/* Top Header Bar */}
          <div className="px-6 py-4 border-b border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/60">
            {/* Brand and Status */}
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-cyan-950/60 border border-cyan-400/40 flex items-center justify-center text-cyan-300">
                <Sparkles className="w-4 h-4" />
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <span className="font-sans font-black text-sm tracking-wider text-white">
                    STELLER-NEBULA CONTROL
                  </span>
                  <span className="px-2 py-0.5 rounded bg-cyan-950/80 border border-cyan-500/40 text-cyan-300 font-mono text-[10px]">
                    LOCAL EDITOR
                  </span>
                </div>
                <div className="font-mono text-[10px] text-slate-400">
                  CONTENT SOURCE // LOCALSTORAGE
                </div>
              </div>
            </div>

            {/* Header Actions */}
            <div className="flex items-center gap-2 self-end sm:self-auto">
              <button
                onClick={handleLockSession}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-white/10 hover:border-white/20 text-slate-400 hover:text-white font-mono text-xs transition-colors"
                title="Lock session"
              >
                <Lock className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">LOCK</span>
              </button>

              <button
                onClick={() => {
                  soundEngine.playButtonClick();
                  onClose();
                }}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-mono text-xs font-bold uppercase transition-all shadow-[0_0_15px_rgba(0,240,255,0.25)]"
              >
                <span>VIEW SITE</span>
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="px-6 border-b border-white/10 bg-slate-950 flex items-center gap-2 overflow-x-auto">
            <button
              onClick={() => {
                soundEngine.playButtonClick();
                setActiveTab('dashboard');
              }}
              className={`flex items-center gap-2 px-4 py-3 font-mono text-xs tracking-wider border-b-2 transition-all shrink-0 ${
                activeTab === 'dashboard'
                  ? 'border-cyan-400 text-cyan-300 bg-cyan-950/20'
                  : 'border-transparent text-slate-400 hover:text-slate-200'
              }`}
            >
              <LayoutDashboard className="w-4 h-4" />
              <span>DASHBOARD</span>
            </button>

            <button
              onClick={() => {
                soundEngine.playButtonClick();
                setActiveTab('projects');
              }}
              className={`flex items-center gap-2 px-4 py-3 font-mono text-xs tracking-wider border-b-2 transition-all shrink-0 ${
                activeTab === 'projects'
                  ? 'border-cyan-400 text-cyan-300 bg-cyan-950/20'
                  : 'border-transparent text-slate-400 hover:text-slate-200'
              }`}
            >
              <Layers className="w-4 h-4" />
              <span>PROJECTS ({projects.length})</span>
            </button>

            <button
              onClick={() => {
                soundEngine.playButtonClick();
                setActiveTab('settings');
              }}
              className={`flex items-center gap-2 px-4 py-3 font-mono text-xs tracking-wider border-b-2 transition-all shrink-0 ${
                activeTab === 'settings'
                  ? 'border-cyan-400 text-cyan-300 bg-cyan-950/20'
                  : 'border-transparent text-slate-400 hover:text-slate-200'
              }`}
            >
              <Settings className="w-4 h-4" />
              <span>SITE SETTINGS</span>
            </button>

            <button
              onClick={() => {
                soundEngine.playButtonClick();
                setActiveTab('backup');
              }}
              className={`flex items-center gap-2 px-4 py-3 font-mono text-xs tracking-wider border-b-2 transition-all shrink-0 ${
                activeTab === 'backup'
                  ? 'border-cyan-400 text-cyan-300 bg-cyan-950/20'
                  : 'border-transparent text-slate-400 hover:text-slate-200'
              }`}
            >
              <Database className="w-4 h-4" />
              <span>BACKUP & RESET</span>
            </button>
          </div>

          {/* Main Tab Content Area */}
          <div className="flex-1 overflow-y-auto p-6 md:p-8 bg-[#06080E]/60">
            {activeTab === 'dashboard' && (
              <AdminDashboardTab
                onNavigate={(tab) => {
                  soundEngine.playButtonClick();
                  setActiveTab(tab);
                }}
                onOpenAddProject={handleOpenAddProject}
                onEditProject={handleEditProjectById}
              />
            )}

            {activeTab === 'projects' && (
              <AdminProjectsTab
                onOpenAdd={handleOpenAddProject}
                onEdit={handleEditProject}
                onShowToast={showToast}
              />
            )}

            {activeTab === 'settings' && (
              <AdminSiteSettingsTab onShowToast={showToast} />
            )}

            {activeTab === 'backup' && (
              <AdminBackupTab onShowToast={showToast} />
            )}
          </div>

          {/* Footer Status Bar */}
          <div className="px-6 py-2.5 border-t border-white/10 bg-slate-950/90 flex items-center justify-between font-mono text-[11px] text-slate-500">
            <div className="flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
              <span>STELLER-NEBULA // LOCAL CONTENT MANAGER</span>
            </div>
            <div className="hidden sm:block">
              Shortcut: <span className="text-cyan-400">Ctrl + Shift + A</span>
            </div>
          </div>
        </div>
      )}

      {/* Project Add / Edit Modal */}
      <AdminProjectModal
        isOpen={isProjectModalOpen}
        projectToEdit={projectToEdit}
        onClose={() => {
          setIsProjectModalOpen(false);
          setProjectToEdit(null);
        }}
        onSaved={(msg) => showToast(msg)}
      />
    </div>
  );
};
