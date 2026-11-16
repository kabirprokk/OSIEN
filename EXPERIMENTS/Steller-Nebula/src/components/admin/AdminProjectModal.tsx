import React, { useState, useEffect } from 'react';
import { X, Sparkles, AlertCircle, Check, Plus, Trash2 } from 'lucide-react';
import { ProjectItem } from '../../types';
import { useContent } from '../../context/ContentContext';
import { soundEngine } from '../../utils/audioSystem';

interface AdminProjectModalProps {
  isOpen: boolean;
  projectToEdit?: ProjectItem | null;
  onClose: () => void;
  onSaved: (message: string) => void;
}

export const AdminProjectModal: React.FC<AdminProjectModalProps> = ({
  isOpen,
  projectToEdit,
  onClose,
  onSaved
}) => {
  const { addProject, updateProject } = useContent();

  const [name, setName] = useState('');
  const [category, setCategory] = useState('');
  const [description, setDescription] = useState('');
  const [tagline, setTagline] = useState('');
  const [url, setUrl] = useState('');
  const [status, setStatus] = useState('Active');
  const [year, setYear] = useState('2026');
  const [features, setFeatures] = useState<string[]>([]);
  const [newFeatureInput, setNewFeatureInput] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (projectToEdit) {
      setName(projectToEdit.name || '');
      setCategory(projectToEdit.category || 'General');
      setDescription(projectToEdit.description || '');
      setTagline(projectToEdit.tagline || '');
      setUrl(projectToEdit.url || '');
      setStatus(projectToEdit.status || 'Active');
      setYear(projectToEdit.year || '2026');
      setFeatures(projectToEdit.features || []);
    } else {
      setName('');
      setCategory('Creative Publishing');
      setDescription('');
      setTagline('');
      setUrl('');
      setStatus('Active');
      setYear(new Date().getFullYear().toString());
      setFeatures([]);
    }
    setError(null);
  }, [projectToEdit, isOpen]);

  if (!isOpen) return null;

  const handleAddFeature = () => {
    if (newFeatureInput.trim()) {
      setFeatures([...features, newFeatureInput.trim()]);
      setNewFeatureInput('');
    }
  };

  const handleRemoveFeature = (idx: number) => {
    setFeatures(features.filter((_, i) => i !== idx));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    soundEngine.playButtonClick();

    if (!name.trim()) {
      setError('Project name is required.');
      return;
    }

    if (!description.trim()) {
      setError('Project description is required.');
      return;
    }

    if (projectToEdit) {
      // Update existing
      const res = updateProject(projectToEdit.id, {
        name: name.trim(),
        category: category.trim() || 'General',
        description: description.trim(),
        tagline: tagline.trim(),
        url: url.trim(),
        status,
        year,
        features
      });

      if (!res.success) {
        setError(res.error || 'Failed to update project.');
        return;
      }

      soundEngine.playEasterEggFanfare();
      onSaved('CHANGES SAVED');
      onClose();
    } else {
      // Add new
      const res = addProject({
        name: name.trim(),
        category: category.trim() || 'General',
        description: description.trim(),
        tagline: tagline.trim(),
        url: url.trim(),
        status,
        year,
        features
      });

      if (!res.success) {
        setError(res.error || 'Failed to add project.');
        return;
      }

      soundEngine.playEasterEggFanfare();
      onSaved('PROJECT ADDED');
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl max-h-[90vh] flex flex-col rounded-2xl border border-cyan-500/30 bg-[#080B12] text-slate-100 shadow-[0_0_80px_rgba(0,240,255,0.15)] overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-white/10 flex items-center justify-between bg-slate-900/50">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-cyan-400" />
            <h3 className="font-sans font-bold text-base text-white">
              {projectToEdit ? `EDIT PROJECT // ${projectToEdit.name}` : 'ADD NEW REAL PROJECT'}
            </h3>
          </div>

          <button
            onClick={() => {
              soundEngine.playButtonClick();
              onClose();
            }}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/5 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Scrollable Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
          {error && (
            <div className="p-3 rounded-lg bg-rose-950/40 border border-rose-500/40 text-rose-300 font-mono text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Row 1: Name & Category */}
          <div className="grid sm:grid-cols-2 gap-4">
            <div>
              <label className="block font-mono text-xs text-slate-300 mb-1.5 uppercase tracking-wider">
                Project Name <span className="text-cyan-400">*</span>
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Storyteller"
                required
                className="w-full px-3.5 py-2.5 rounded-lg bg-slate-900 border border-white/15 focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400 text-white font-sans text-sm placeholder-slate-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block font-mono text-xs text-slate-300 mb-1.5 uppercase tracking-wider">
                Category
              </label>
              <input
                type="text"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="e.g. Creative Publishing"
                className="w-full px-3.5 py-2.5 rounded-lg bg-slate-900 border border-white/15 focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400 text-white font-sans text-sm placeholder-slate-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Tagline / Subtitle */}
          <div>
            <label className="block font-mono text-xs text-slate-300 mb-1.5 uppercase tracking-wider">
              Tagline (Optional)
            </label>
            <input
              type="text"
              value={tagline}
              onChange={(e) => setTagline(e.target.value)}
              placeholder="e.g. Stories that stay."
              className="w-full px-3.5 py-2.5 rounded-lg bg-slate-900 border border-white/15 focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400 text-white font-sans text-sm placeholder-slate-500 focus:outline-none"
            />
          </div>

          {/* Description */}
          <div>
            <label className="block font-mono text-xs text-slate-300 mb-1.5 uppercase tracking-wider">
              Description <span className="text-cyan-400">*</span>
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="A cinematic publishing platform for honest voices, untold worlds, and stories that stay."
              rows={3}
              required
              className="w-full px-3.5 py-2.5 rounded-lg bg-slate-900 border border-white/15 focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400 text-white font-sans text-sm placeholder-slate-500 focus:outline-none resize-none leading-relaxed"
            />
          </div>

          {/* Row 2: URL & Status */}
          <div className="grid sm:grid-cols-3 gap-4">
            <div className="sm:col-span-2">
              <label className="block font-mono text-xs text-slate-300 mb-1.5 uppercase tracking-wider">
                Project URL (Optional)
              </label>
              <input
                type="url"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="https://..."
                className="w-full px-3.5 py-2.5 rounded-lg bg-slate-900 border border-white/15 focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400 text-white font-sans text-sm placeholder-slate-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block font-mono text-xs text-slate-300 mb-1.5 uppercase tracking-wider">
                Status
              </label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-lg bg-slate-900 border border-white/15 focus:border-cyan-400 text-white font-sans text-sm focus:outline-none"
              >
                <option value="Active">Active</option>
                <option value="In development">In development</option>
                <option value="Archived">Archived</option>
              </select>
            </div>
          </div>

          {/* Feature Highlights */}
          <div className="space-y-2 pt-2 border-t border-white/5">
            <label className="block font-mono text-xs text-slate-300 uppercase tracking-wider">
              Project Highlights (Optional)
            </label>

            <div className="flex gap-2">
              <input
                type="text"
                value={newFeatureInput}
                onChange={(e) => setNewFeatureInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddFeature();
                  }
                }}
                placeholder="e.g. Cinematic reading experience"
                className="flex-1 px-3 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-xs focus:outline-none focus:border-cyan-400"
              />
              <button
                type="button"
                onClick={handleAddFeature}
                className="px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-cyan-300 font-mono text-xs border border-white/10 flex items-center gap-1"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add</span>
              </button>
            </div>

            {features.length > 0 && (
              <div className="flex flex-wrap gap-2 pt-2">
                {features.map((feat, idx) => (
                  <span
                    key={idx}
                    className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-slate-900 border border-white/10 text-slate-200 text-xs font-sans"
                  >
                    <span>{feat}</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveFeature(idx)}
                      className="text-slate-400 hover:text-rose-400"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>
        </form>

        {/* Footer Actions */}
        <div className="px-6 py-4 border-t border-white/10 flex items-center justify-end gap-3 bg-slate-900/60">
          <button
            type="button"
            onClick={() => {
              soundEngine.playButtonClick();
              onClose();
            }}
            className="px-4 py-2 rounded-lg border border-white/10 hover:border-white/20 text-slate-300 hover:text-white font-mono text-xs transition-colors"
          >
            CANCEL
          </button>

          <button
            type="button"
            onClick={handleSubmit}
            className="px-5 py-2 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-mono text-xs font-bold tracking-wider uppercase transition-all shadow-[0_0_15px_rgba(0,240,255,0.3)]"
          >
            {projectToEdit ? 'SAVE CHANGES' : 'CREATE PROJECT'}
          </button>
        </div>
      </div>
    </div>
  );
};
