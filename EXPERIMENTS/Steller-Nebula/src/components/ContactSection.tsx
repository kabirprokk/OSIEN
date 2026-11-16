import React, { useState } from 'react';
import { Mail, Copy, Check, ExternalLink, Github, Twitter, ArrowUpRight, Signal } from 'lucide-react';
import { useContent } from '../context/ContentContext';
import { soundEngine } from '../utils/audioSystem';

export const ContactSection: React.FC = () => {
  const { studioConfig } = useContent();
  const [copiedEmail, setCopiedEmail] = useState(false);

  const handleCopyEmail = () => {
    soundEngine.playButtonClick();
    navigator.clipboard.writeText(studioConfig.contactEmail);
    setCopiedEmail(true);
    setTimeout(() => setCopiedEmail(false), 2500);
  };

  const getSocialIcon = (platform: string) => {
    switch (platform.toLowerCase()) {
      case 'github':
        return <Github className="w-4 h-4" />;
      case 'x / twitter':
      case 'twitter':
      case 'x':
        return <Twitter className="w-4 h-4" />;
      default:
        return <ExternalLink className="w-4 h-4" />;
    }
  };

  return (
    <section
      id="contact"
      className="relative py-28 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto select-none overflow-hidden"
    >
      {/* Section Header */}
      <div className="relative z-10 space-y-2 mb-14 border-b border-white/5 pb-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 font-mono text-[11px] text-teal-400">
            <span className="w-1.5 h-1.5 rounded-full bg-teal-400" />
            <span className="tracking-widest uppercase">GET IN TOUCH</span>
          </div>

          <div className="font-mono text-[10px] text-teal-300 bg-teal-950/40 border border-teal-500/30 px-2.5 py-0.5 rounded flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-teal-400" />
            <span>AVAILABLE</span>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
          <div>
            <h2 className="text-3xl sm:text-4xl font-sans font-bold tracking-tight text-white uppercase drop-shadow-[0_0_20px_rgba(255,255,255,0.3)]">
              CONTACT
            </h2>
            <p className="text-sm sm:text-base text-slate-300 font-sans mt-1">
              Have a question, an idea, or want to collaborate?
            </p>
          </div>
        </div>
      </div>

      {/* Grid: Direct Email Card + Links */}
      <div className="grid md:grid-cols-12 gap-6 items-stretch relative z-10">
        {/* Left: Email Card */}
        <div className="md:col-span-7 rounded-xl border border-white/15 hover:border-teal-500/40 bg-[#050811]/85 backdrop-blur-xl p-8 sm:p-10 space-y-6 flex flex-col justify-between shadow-[0_0_40px_rgba(0,0,0,0.5)]">
          <div className="space-y-4">
            <div className="flex items-center justify-between font-mono text-[10px] text-teal-300 tracking-wider font-semibold">
              <span>DIRECT EMAIL</span>
              <Mail className="w-4 h-4 text-teal-400" />
            </div>

            <div>
              <div className="font-mono text-[11px] text-teal-300 font-bold tracking-wider">
                EMAIL ADDRESS
              </div>
              <div className="font-sans font-bold text-xl sm:text-2xl text-white mt-1 break-all select-all drop-shadow-[0_0_15px_rgba(255,255,255,0.2)]">
                {studioConfig.contactEmail}
              </div>
            </div>

            <p className="text-sm text-slate-200 font-sans leading-relaxed">
              Feel free to reach out directly. I'm always open to discussing new projects, creative ideas, or questions.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3 pt-4 border-t border-white/10">
            <button
              onClick={handleCopyEmail}
              onMouseEnter={() => soundEngine.playHoverBlip(880)}
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-teal-400 hover:bg-teal-300 text-slate-950 font-mono text-xs font-bold tracking-wider uppercase transition-all duration-200 shadow-[0_0_20px_rgba(45,212,191,0.35)] cursor-pointer"
            >
              {copiedEmail ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copiedEmail ? 'COPIED' : 'COPY EMAIL'}</span>
            </button>

            <a
              href={`mailto:${studioConfig.contactEmail}`}
              onClick={() => soundEngine.playButtonClick()}
              className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-lg border border-white/15 hover:border-teal-400 text-slate-200 font-mono text-xs hover:text-white transition-colors cursor-pointer bg-[#030509]/60"
            >
              <span>SEND EMAIL</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </a>
          </div>
        </div>

        {/* Right: Profiles & Links */}
        <div className="md:col-span-5 rounded-xl border border-white/15 bg-[#050811]/75 backdrop-blur-lg p-7 space-y-4 flex flex-col justify-between">
          <div>
            <div className="font-mono text-[10px] text-slate-300 tracking-wider mb-4 font-semibold">
              ONLINE PROFILES
            </div>

            <div className="space-y-2.5">
              {studioConfig.socials.map((social) => {
                if (!social.url || social.url === '#') return null;
                return (
                  <a
                    key={social.platform}
                    href={social.url}
                    target="_blank"
                    rel="noreferrer"
                    onMouseEnter={() => soundEngine.playHoverBlip(920)}
                    onClick={() => soundEngine.playButtonClick()}
                    className="flex items-center justify-between p-3 rounded-lg border border-white/10 hover:border-teal-500/40 bg-[#030509]/75 hover:bg-teal-950/30 text-slate-200 hover:text-teal-200 transition-all group cursor-pointer"
                  >
                    <div className="flex items-center gap-2.5">
                      <div className="p-1 rounded bg-[#050811] text-teal-400 group-hover:text-teal-300">
                        {getSocialIcon(social.platform)}
                      </div>
                      <span className="font-sans font-semibold text-xs text-white group-hover:text-teal-200">
                        {social.platform}
                      </span>
                    </div>

                    <ArrowUpRight className="w-3.5 h-3.5 text-slate-400 group-hover:text-teal-300 transition-colors" />
                  </a>
                );
              })}
            </div>
          </div>

          <div className="pt-3 border-t border-white/10 font-mono text-[10px] text-slate-400">
            STELLER-NEBULA · {studioConfig.year}
          </div>
        </div>
      </div>
    </section>
  );
};
