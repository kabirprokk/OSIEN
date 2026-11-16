import React, { useState } from 'react';
import { Lock, Key, ArrowRight, X, AlertCircle, ShieldAlert } from 'lucide-react';
import { ADMIN_CONFIG } from '../../config/adminConfig';
import { soundEngine } from '../../utils/audioSystem';

interface AdminLockScreenProps {
  onUnlock: () => void;
  onClose: () => void;
}

export const AdminLockScreen: React.FC<AdminLockScreenProps> = ({ onUnlock, onClose }) => {
  const [passwordInput, setPasswordInput] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    soundEngine.playButtonClick();

    if (!passwordInput.trim()) {
      setError('Please enter the access key.');
      return;
    }

    setIsSubmitting(true);
    // Casual-access password check
    if (passwordInput === ADMIN_CONFIG.password) {
      soundEngine.playEasterEggFanfare();
      setError(null);
      onUnlock();
    } else {
      soundEngine.playHoverBlip(300);
      setError('Invalid access key. Please verify and try again.');
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative w-full max-w-md mx-auto p-8 rounded-2xl border border-cyan-500/30 bg-[#080B12]/95 backdrop-blur-2xl shadow-[0_0_80px_rgba(0,240,255,0.15)] text-slate-100 animate-in fade-in zoom-in-95 duration-200">
      {/* Top Close Button */}
      <button
        onClick={() => {
          soundEngine.playButtonClick();
          onClose();
        }}
        className="absolute top-4 right-4 p-2 text-slate-400 hover:text-white rounded-lg hover:bg-white/5 transition-colors"
        title="Exit Admin"
        aria-label="Close"
      >
        <X className="w-4 h-4" />
      </button>

      {/* Header Emblem */}
      <div className="flex flex-col items-center text-center space-y-3 mb-8">
        <div className="w-12 h-12 rounded-xl bg-cyan-950/60 border border-cyan-400/40 flex items-center justify-center text-cyan-300 shadow-[0_0_20px_rgba(0,240,255,0.25)]">
          <Lock className="w-5 h-5" />
        </div>

        <div>
          <span className="font-mono text-[10px] tracking-[0.25em] text-cyan-400 uppercase block">
            STELLER-NEBULA
          </span>
          <h2 className="text-xl sm:text-2xl font-sans font-black text-white tracking-tight mt-0.5">
            LOCAL ADMIN ACCESS
          </h2>
        </div>

        <p className="font-sans text-xs text-slate-400 max-w-xs">
          Enter access key to manage local projects, site copy, and exports.
        </p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block font-mono text-[11px] text-slate-300 uppercase tracking-wider mb-2">
            Access Key
          </label>
          <div className="relative">
            <input
              type="password"
              value={passwordInput}
              onChange={(e) => {
                setPasswordInput(e.target.value);
                if (error) setError(null);
              }}
              placeholder="Enter password..."
              autoFocus
              className="w-full px-4 py-3 rounded-lg bg-slate-900/90 border border-white/15 focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400 text-white font-mono text-sm placeholder-slate-500 focus:outline-none transition-all"
            />
            <Key className="w-4 h-4 absolute right-3.5 top-3.5 text-slate-500 pointer-events-none" />
          </div>
        </div>

        {error && (
          <div className="flex items-center gap-2 p-3 rounded-lg bg-rose-950/40 border border-rose-500/40 text-rose-300 font-mono text-xs animate-in fade-in">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
            <span>{error}</span>
          </div>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full inline-flex items-center justify-center gap-2 py-3 px-4 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-mono text-xs font-bold tracking-wider uppercase transition-all duration-200 shadow-[0_0_20px_rgba(0,240,255,0.3)] hover:shadow-[0_0_30px_rgba(0,240,255,0.5)] focus:outline-none"
        >
          <span>ENTER CONTROL PANEL</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </form>

      {/* Disclaimer */}
      <div className="mt-8 pt-4 border-t border-white/10 text-center font-mono text-[10px] text-slate-400 space-y-1">
        <div>Frontend-only editor // Local session</div>
        <div className="text-slate-400">Shortcut: <span className="text-cyan-400 font-bold">Ctrl + Shift + A</span></div>
      </div>
    </div>
  );
};
