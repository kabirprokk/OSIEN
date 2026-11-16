import React, { useState, useEffect } from 'react';
import { Save, Sparkles, Plus, Trash2, User, Compass, Info } from 'lucide-react';
import { useContent } from '../../context/ContentContext';
import { StudioConfig, SocialLink } from '../../types';
import { soundEngine } from '../../utils/audioSystem';

interface AdminSiteSettingsTabProps {
  onShowToast: (message: string) => void;
}

export const AdminSiteSettingsTab: React.FC<AdminSiteSettingsTabProps> = ({ onShowToast }) => {
  const { studioConfig, updateStudioConfig } = useContent();

  const [formState, setFormState] = useState<StudioConfig>(studioConfig);
  const [interestsInput, setInterestsInput] = useState<string>(
    (studioConfig.developer?.interests || []).join(', ')
  );

  useEffect(() => {
    setFormState(studioConfig);
    setInterestsInput((studioConfig.developer?.interests || []).join(', '));
  }, [studioConfig]);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    soundEngine.playEasterEggFanfare();

    // Parse interests
    const parsedInterests = interestsInput
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    const updatedConfig: StudioConfig = {
      ...formState,
      developer: {
        ...formState.developer,
        interests: parsedInterests
      }
    };

    updateStudioConfig(updatedConfig);
    onShowToast('SETTINGS SAVED');
  };

  const handleSocialChange = (index: number, field: keyof SocialLink, value: string) => {
    const updated = [...formState.socials];
    updated[index] = {
      ...updated[index],
      [field]: value
    };
    setFormState({ ...formState, socials: updated });
  };

  const handleAddSocial = () => {
    setFormState({
      ...formState,
      socials: [
        ...formState.socials,
        { platform: 'New Link', url: 'https://', handle: 'handle', icon: 'Link' }
      ]
    });
  };

  const handleRemoveSocial = (index: number) => {
    setFormState({
      ...formState,
      socials: formState.socials.filter((_, i) => i !== index)
    });
  };

  const handleHowIBuildChange = (index: number, field: 'title' | 'description', value: string) => {
    const updated = [...(formState.howIBuild || [])];
    updated[index] = {
      ...updated[index],
      [field]: value
    };
    setFormState({ ...formState, howIBuild: updated });
  };

  return (
    <form onSubmit={handleSave} className="space-y-8 animate-in fade-in duration-200">
      {/* Header & Save CTA */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/10 pb-4">
        <div>
          <h3 className="font-sans font-bold text-lg text-white">
            STUDIO SETTINGS & INFORMATION
          </h3>
          <p className="font-sans text-xs text-slate-400">
            Control genuine developer information, about narratives, contact details, and links.
          </p>
        </div>

        <button
          type="submit"
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-mono text-xs font-bold tracking-wider uppercase transition-all shadow-[0_0_15px_rgba(0,240,255,0.3)] self-start sm:self-auto cursor-pointer"
        >
          <Save className="w-4 h-4" />
          <span>SAVE SETTINGS</span>
        </button>
      </div>

      {/* 1. Identity & Hero */}
      <div className="space-y-4 p-6 rounded-xl border border-white/10 bg-slate-900/40">
        <h4 className="font-mono text-xs text-cyan-400 uppercase tracking-wider flex items-center gap-2">
          <Sparkles className="w-3.5 h-3.5" />
          <span>1. BRAND & HERO SECTION</span>
        </h4>

        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="block font-mono text-xs text-slate-300 mb-1">
              Studio / Brand Name
            </label>
            <input
              type="text"
              value={formState.studioName}
              onChange={(e) => setFormState({ ...formState, studioName: e.target.value })}
              className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-cyan-400 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-mono text-xs text-slate-300 mb-1">
              Hero Headline
            </label>
            <input
              type="text"
              value={formState.heroHeadline}
              onChange={(e) => setFormState({ ...formState, heroHeadline: e.target.value })}
              className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-cyan-400 focus:outline-none"
            />
          </div>
        </div>

        <div>
          <label className="block font-mono text-xs text-slate-300 mb-1">
            Hero Subtitle
          </label>
          <input
            type="text"
            value={formState.heroSubtitle}
            onChange={(e) => setFormState({ ...formState, heroSubtitle: e.target.value })}
            className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-cyan-400 focus:outline-none"
          />
        </div>

        <div>
          <label className="block font-mono text-xs text-slate-300 mb-1">
            Hero Supporting Text
          </label>
          <textarea
            value={formState.heroSupportingText}
            onChange={(e) => setFormState({ ...formState, heroSupportingText: e.target.value })}
            rows={2}
            className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-cyan-400 focus:outline-none resize-none"
          />
        </div>
      </div>

      {/* 2. The Developer Information */}
      <div className="space-y-4 p-6 rounded-xl border border-white/10 bg-slate-900/40">
        <h4 className="font-mono text-xs text-teal-300 uppercase tracking-wider flex items-center gap-2">
          <User className="w-3.5 h-3.5 text-teal-400" />
          <span>2. THE DEVELOPER</span>
        </h4>

        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="block font-mono text-xs text-slate-300 mb-1">
              Developer Name
            </label>
            <input
              type="text"
              value={formState.developer?.name || ''}
              onChange={(e) =>
                setFormState({
                  ...formState,
                  developer: { ...formState.developer, name: e.target.value }
                })
              }
              placeholder="e.g. Kabir Sayed"
              className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-teal-400 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-mono text-xs text-slate-300 mb-1">
              Profile Image URL (Optional)
            </label>
            <input
              type="url"
              value={formState.developer?.avatarUrl || ''}
              onChange={(e) =>
                setFormState({
                  ...formState,
                  developer: { ...formState.developer, avatarUrl: e.target.value }
                })
              }
              placeholder="https://... (Leave empty if none)"
              className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-teal-400 focus:outline-none"
            />
          </div>
        </div>

        <div>
          <label className="block font-mono text-xs text-slate-300 mb-1">
            Developer Bio (Honest, Personal)
          </label>
          <textarea
            value={formState.developer?.bio || ''}
            onChange={(e) =>
              setFormState({
                ...formState,
                developer: { ...formState.developer, bio: e.target.value }
              })
            }
            rows={3}
            placeholder="I'm a developer who likes turning ideas into things people can actually use..."
            className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-teal-400 focus:outline-none resize-none leading-relaxed"
          />
        </div>

        <div>
          <label className="block font-mono text-xs text-slate-300 mb-1">
            Focus & Interests (Comma-separated)
          </label>
          <input
            type="text"
            value={interestsInput}
            onChange={(e) => setInterestsInput(e.target.value)}
            placeholder="Software Development, Creative Technology, Web Applications"
            className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-teal-400 focus:outline-none"
          />
        </div>
      </div>

      {/* 3. About Section Content */}
      <div className="space-y-4 p-6 rounded-xl border border-white/10 bg-slate-900/40">
        <h4 className="font-mono text-xs text-purple-300 uppercase tracking-wider flex items-center gap-2">
          <Info className="w-3.5 h-3.5 text-purple-400" />
          <span>3. ABOUT SECTION CONTENT</span>
        </h4>

        <div>
          <label className="block font-mono text-xs text-slate-300 mb-1">
            About Section Intro Tagline
          </label>
          <input
            type="text"
            value={formState.aboutIntro || ''}
            onChange={(e) => setFormState({ ...formState, aboutIntro: e.target.value })}
            placeholder="A place for the things I'm building."
            className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-purple-400 focus:outline-none"
          />
        </div>

        <div>
          <label className="block font-mono text-xs text-slate-300 mb-1">
            What is Steller-Nebula? (Narrative)
          </label>
          <textarea
            value={formState.aboutText}
            onChange={(e) => setFormState({ ...formState, aboutText: e.target.value })}
            rows={3}
            className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-purple-400 focus:outline-none resize-none leading-relaxed"
          />
        </div>

        <div>
          <label className="block font-mono text-xs text-slate-300 mb-1">
            Why It Exists
          </label>
          <textarea
            value={formState.whyItExists || ''}
            onChange={(e) => setFormState({ ...formState, whyItExists: e.target.value })}
            rows={2}
            placeholder="I wanted one place for the things I build instead of treating every project as something completely separate."
            className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-purple-400 focus:outline-none resize-none leading-relaxed"
          />
        </div>

        <div>
          <label className="block font-mono text-xs text-slate-300 mb-1">
            What's Next
          </label>
          <textarea
            value={formState.whatsNext || ''}
            onChange={(e) => setFormState({ ...formState, whatsNext: e.target.value })}
            rows={2}
            placeholder="Steller-Nebula will grow alongside the projects behind it. New work will appear here when it is ready to be shared."
            className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-purple-400 focus:outline-none resize-none leading-relaxed"
          />
        </div>
      </div>

      {/* 4. Development Philosophy */}
      <div className="space-y-4 p-6 rounded-xl border border-white/10 bg-slate-900/40">
        <h4 className="font-mono text-xs text-purple-300 uppercase tracking-wider flex items-center gap-2">
          <Compass className="w-3.5 h-3.5 text-purple-400" />
          <span>4. HOW I BUILD (STATEMENTS)</span>
        </h4>

        {(formState.howIBuild || []).map((item, idx) => (
          <div key={idx} className="p-3.5 rounded-lg bg-black/40 border border-white/10 space-y-2">
            <input
              type="text"
              value={item.title}
              onChange={(e) => handleHowIBuildChange(idx, 'title', e.target.value)}
              placeholder="Statement title (e.g. Build things, don't just plan them.)"
              className="w-full px-3 py-1.5 rounded bg-slate-900 border border-white/15 text-white font-sans text-xs focus:border-purple-400 focus:outline-none font-bold"
            />
            <textarea
              value={item.description}
              onChange={(e) => handleHowIBuildChange(idx, 'description', e.target.value)}
              rows={2}
              placeholder="Statement description"
              className="w-full px-3 py-1.5 rounded bg-slate-900 border border-white/15 text-slate-200 font-sans text-xs focus:border-purple-400 focus:outline-none resize-none"
            />
          </div>
        ))}
      </div>

      {/* 5. Contact & Socials */}
      <div className="space-y-4 p-6 rounded-xl border border-white/10 bg-slate-900/40">
        <h4 className="font-mono text-xs text-cyan-400 uppercase tracking-wider flex items-center gap-2">
          <Sparkles className="w-3.5 h-3.5" />
          <span>5. CONTACT & PROFILES</span>
        </h4>

        <div>
          <label className="block font-mono text-xs text-slate-300 mb-1">
            Primary Contact Email
          </label>
          <input
            type="email"
            value={formState.contactEmail}
            onChange={(e) => setFormState({ ...formState, contactEmail: e.target.value })}
            className="w-full px-3.5 py-2 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-sm focus:border-cyan-400 focus:outline-none"
          />
        </div>

        {/* Social Links List */}
        <div className="space-y-3 pt-2">
          <div className="flex items-center justify-between">
            <label className="font-mono text-xs text-slate-300 uppercase">
              External Profiles / Links
            </label>
            <button
              type="button"
              onClick={handleAddSocial}
              className="text-cyan-400 hover:text-cyan-300 font-mono text-xs flex items-center gap-1 cursor-pointer"
            >
              <Plus className="w-3 h-3" />
              <span>Add Link</span>
            </button>
          </div>

          {formState.socials.map((social, idx) => (
            <div key={idx} className="flex items-center gap-3">
              <input
                type="text"
                value={social.platform}
                onChange={(e) => handleSocialChange(idx, 'platform', e.target.value)}
                placeholder="Platform"
                className="w-1/3 px-3 py-1.5 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-xs focus:border-cyan-400 focus:outline-none"
              />
              <input
                type="url"
                value={social.url}
                onChange={(e) => handleSocialChange(idx, 'url', e.target.value)}
                placeholder="URL (https://...)"
                className="flex-1 px-3 py-1.5 rounded-lg bg-slate-900 border border-white/15 text-white font-sans text-xs focus:border-cyan-400 focus:outline-none"
              />
              <button
                type="button"
                onClick={() => handleRemoveSocial(idx)}
                className="p-1.5 rounded text-slate-500 hover:text-rose-400 cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Save Button */}
      <div className="pt-2 flex justify-end">
        <button
          type="submit"
          className="inline-flex items-center gap-2 px-6 py-3 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-mono text-xs font-bold tracking-wider uppercase transition-all shadow-[0_0_20px_rgba(0,240,255,0.3)] cursor-pointer"
        >
          <Save className="w-4 h-4" />
          <span>SAVE ALL CHANGES</span>
        </button>
      </div>
    </form>
  );
};
