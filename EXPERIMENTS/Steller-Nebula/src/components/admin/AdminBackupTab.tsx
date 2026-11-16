import React, { useState, useRef } from 'react';
import { Download, Upload, RotateCcw, AlertTriangle, FileText, CheckCircle2, AlertCircle } from 'lucide-react';
import { useContent } from '../../context/ContentContext';
import { soundEngine } from '../../utils/audioSystem';

interface AdminBackupTabProps {
  onShowToast: (message: string) => void;
}

export const AdminBackupTab: React.FC<AdminBackupTabProps> = ({ onShowToast }) => {
  const { exportBackup, importBackup, resetToDefault, isUsingCustomData } = useContent();

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [showResetConfirm, setShowResetConfirm] = useState(false);
  const [importStatus, setImportStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const handleExport = () => {
    soundEngine.playEasterEggFanfare();
    exportBackup();
    onShowToast('BACKUP EXPORTED');
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      if (content) {
        const result = importBackup(content);
        if (result.success) {
          soundEngine.playEasterEggFanfare();
          setImportStatus({ type: 'success', message: 'Backup successfully imported and applied!' });
          onShowToast('BACKUP RESTORED');
        } else {
          soundEngine.playHoverBlip(300);
          setImportStatus({ type: 'error', message: result.error || 'Failed to import backup.' });
        }
      }
    };
    reader.readAsText(file);
    // Reset file input
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleResetConfirm = () => {
    soundEngine.playButtonClick();
    resetToDefault();
    setShowResetConfirm(false);
    onShowToast('RESET TO DEFAULT');
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-200">
      {/* Header */}
      <div className="border-b border-white/10 pb-4">
        <h3 className="font-sans font-bold text-lg text-white">
          BACKUP, IMPORT & RESTORE
        </h3>
        <p className="font-sans text-xs text-slate-400">
          Export your verified projects and configuration as JSON, transfer to other devices, or reset.
        </p>
      </div>

      {importStatus && (
        <div
          className={`p-4 rounded-xl border flex items-center gap-3 animate-in fade-in ${
            importStatus.type === 'success'
              ? 'bg-emerald-950/40 border-emerald-500/40 text-emerald-300'
              : 'bg-rose-950/40 border-rose-500/40 text-rose-300'
          }`}
        >
          {importStatus.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 shrink-0" />
          ) : (
            <AlertCircle className="w-5 h-5 shrink-0" />
          )}
          <span className="font-mono text-xs">{importStatus.message}</span>
        </div>
      )}

      <div className="grid md:grid-cols-2 gap-6">
        {/* 1. Export Card */}
        <div className="p-6 rounded-xl border border-cyan-500/20 bg-slate-900/40 space-y-4 flex flex-col justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 font-mono text-xs text-cyan-400">
              <Download className="w-4 h-4" />
              <span>1. EXPORT BACKUP</span>
            </div>
            <h4 className="font-sans font-bold text-base text-white">
              Download JSON Snapshot
            </h4>
            <p className="font-sans text-xs text-slate-400 leading-relaxed">
              Downloads a complete JSON file containing all active projects, categories, descriptions, URLs, and studio settings.
            </p>
          </div>

          <button
            onClick={handleExport}
            className="w-full inline-flex items-center justify-center gap-2 py-3 px-4 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-mono text-xs font-bold uppercase tracking-wider transition-all shadow-[0_0_15px_rgba(0,240,255,0.2)]"
          >
            <Download className="w-4 h-4" />
            <span>EXPORT JSON FILE</span>
          </button>
        </div>

        {/* 2. Import Card */}
        <div className="p-6 rounded-xl border border-white/10 bg-slate-900/40 space-y-4 flex flex-col justify-between">
          <div className="space-y-2">
            <div className="flex items-center gap-2 font-mono text-xs text-slate-300">
              <Upload className="w-4 h-4 text-cyan-400" />
              <span>2. IMPORT BACKUP</span>
            </div>
            <h4 className="font-sans font-bold text-base text-white">
              Restore from JSON
            </h4>
            <p className="font-sans text-xs text-slate-400 leading-relaxed">
              Upload a previously exported backup file to restore projects and configuration into this browser's local storage.
            </p>
          </div>

          <div>
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileUpload}
              accept=".json,application/json"
              className="hidden"
            />
            <button
              onClick={() => fileInputRef.current?.click()}
              className="w-full inline-flex items-center justify-center gap-2 py-3 px-4 rounded-lg border border-white/15 hover:border-cyan-400 bg-slate-800 hover:bg-cyan-950/40 text-cyan-300 hover:text-white font-mono text-xs font-bold uppercase tracking-wider transition-all"
            >
              <Upload className="w-4 h-4" />
              <span>CHOOSE JSON FILE</span>
            </button>
          </div>
        </div>
      </div>

      {/* 3. Reset Section */}
      <div className="p-6 rounded-xl border border-rose-500/20 bg-rose-950/10 space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2 font-mono text-xs text-rose-400">
              <RotateCcw className="w-4 h-4" />
              <span>3. RESET LOCAL CHANGES</span>
            </div>
            <h4 className="font-sans font-bold text-base text-white">
              Restore Default Codebase State
            </h4>
            <p className="font-sans text-xs text-slate-400 max-w-xl leading-relaxed">
              Clears all local storage overrides in this browser and reverts the website back to the original default configuration (Storyteller project).
            </p>
          </div>

          <button
            onClick={() => {
              soundEngine.playButtonClick();
              setShowResetConfirm(true);
            }}
            className="px-4 py-2.5 rounded-lg border border-rose-500/40 hover:border-rose-400 bg-rose-950/40 text-rose-300 hover:text-rose-100 font-mono text-xs font-bold uppercase transition-colors shrink-0"
          >
            RESET TO DEFAULT
          </button>
        </div>
      </div>

      {/* Reset Confirmation Modal */}
      {showResetConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="relative w-full max-w-md p-6 rounded-2xl border border-rose-500/40 bg-[#080B12] text-slate-100 shadow-2xl space-y-5">
            <div className="flex items-start gap-3">
              <div className="p-2 rounded-lg bg-rose-950/60 border border-rose-500/30 text-rose-400">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div className="space-y-1">
                <h4 className="font-sans font-bold text-base text-white">
                  RESET ALL LOCAL CHANGES?
                </h4>
                <p className="font-sans text-xs text-slate-400 leading-relaxed">
                  This will erase custom projects and copy stored in your browser's local storage and restore default Steller-Nebula data.
                </p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setShowResetConfirm(false)}
                className="px-4 py-2 rounded-lg border border-white/10 text-slate-300 hover:text-white font-mono text-xs transition-colors"
              >
                CANCEL
              </button>
              <button
                onClick={handleResetConfirm}
                className="px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-mono text-xs font-bold uppercase transition-colors shadow-lg shadow-rose-950"
              >
                CONFIRM RESET
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
