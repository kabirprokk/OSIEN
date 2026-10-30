"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Package, PackageType } from "@/lib/registry";
import { PackageCard } from "@/components/PackageCard";
import { CheckIcon } from "@/components/TypeIcon";

const TOPICS = [
  {
    id: "build",
    title: "Build apps",
    detail: "Code, UI, and implementation",
    keywords: ["coding", "engineering", "implementation", "frontend", "backend", "web", "python", "javascript", "typescript", "react", "nextjs", "design", "ui", "ux", "mobile", "prototype", "animation"],
  },
  {
    id: "automate",
    title: "Automate work",
    detail: "Agents, workflows, and integrations",
    keywords: ["automation", "agents", "agent", "workflow", "mcp", "cli", "productivity", "tools"],
  },
  {
    id: "debug",
    title: "Test and debug",
    detail: "Find issues and improve quality",
    keywords: ["debugging", "testing", "review", "verification", "quality", "troubleshooting", "qa"],
  },
  {
    id: "data-cloud",
    title: "Work with data and cloud",
    detail: "Databases, infrastructure, and deployment",
    keywords: ["database", "postgres", "sql", "neon", "prisma", "cloud", "azure", "cloudflare", "infrastructure", "deployment", "devops"],
  },
  {
    id: "media",
    title: "Create media",
    detail: "Video, images, audio, and editing",
    keywords: ["video", "images", "image", "audio", "music", "generation", "editing", "runcomfy", "hyperframes", "remotion"],
  },
  {
    id: "writing-planning",
    title: "Write and plan",
    detail: "Docs, research, specs, and project work",
    keywords: ["documentation", "writing", "planning", "research", "specification", "tickets", "handoff", "github"],
  },
] as const;

const TYPE_LABELS: Record<string, string> = {
  skill: "Skills",
  agent: "Agents",
  command: "Commands",
  "skill-pack": "Skill packs",
  mcp: "MCP servers",
};

function tokens(value: string) {
  return new Set(value.toLowerCase().split(/[^a-z0-9]+/).filter(Boolean));
}

function recommend(packages: Package[], selectedTopics: string[], type: PackageType | "all") {
  return packages
    .filter((pkg) => type === "all" || pkg.type === type)
    .map((pkg) => {
      const tagTokens = tokens(pkg.tags.join(" "));
      const textTokens = tokens(`${pkg.name} ${pkg.description} ${pkg.author}`);
      const matchedTopics = TOPICS.filter((topic) => selectedTopics.includes(topic.id)).filter((topic) => {
        const tagHits = topic.keywords.filter((keyword) => tagTokens.has(keyword)).length;
        const textHits = topic.keywords.filter((keyword) => textTokens.has(keyword) && !tagTokens.has(keyword)).length;
        return tagHits > 0 || textHits > 0;
      });
      const score = matchedTopics.reduce((total, topic) => {
        const tagHits = topic.keywords.filter((keyword) => tagTokens.has(keyword)).length;
        const textHits = topic.keywords.filter((keyword) => textTokens.has(keyword) && !tagTokens.has(keyword)).length;
        return total + tagHits * 5 + textHits;
      }, 0);
      return { pkg, score, reasons: matchedTopics.map((topic) => topic.title) };
    })
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score || a.pkg.name.localeCompare(b.pkg.name))
}

export function PackageFinder({ packages }: { packages: Package[] }) {
  const [selectedTopics, setSelectedTopics] = useState<string[]>([]);
  const [selectedType, setSelectedType] = useState<PackageType | "all">("all");
  const [submitted, setSubmitted] = useState(false);
  const skills = useMemo(
    () => packages.filter((pkg) => pkg.type === "skill").sort((a, b) => a.name.localeCompare(b.name)),
    [packages],
  );
  const packageTypes = useMemo(() => [...new Set(packages.map((pkg) => pkg.type))], [packages]);
  const matches = useMemo(
    () => recommend(packages, selectedTopics, selectedType),
    [packages, selectedTopics, selectedType],
  );

  function toggleTopic(id: string) {
    setSubmitted(false);
    setSelectedTopics((current) => {
      if (current.includes(id)) return current.filter((topic) => topic !== id);
      if (current.length >= 3) return current;
      return [...current, id];
    });
  }

  function reset() {
    setSelectedTopics([]);
    setSelectedType("all");
    setSubmitted(false);
  }

  return (
    <>
      <section className="relative isolate overflow-hidden border-b border-[var(--line)]">
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_78%_20%,color-mix(in_srgb,var(--accent)_10%,transparent),transparent_38%)]" />
        <div className="mx-auto grid max-w-7xl items-center gap-10 px-5 py-14 sm:px-6 sm:py-20 lg:grid-cols-[1.08fr_0.92fr] lg:gap-12 lg:py-24">
          <div className="max-w-2xl">
            <p className="font-mono text-[11px] font-semibold uppercase tracking-[0.2em] text-[var(--accent-text)]">OpenCode Marketplace</p>
            <h1 className="mt-5 max-w-xl font-serif text-5xl font-medium leading-[1.02] tracking-[-0.045em] text-[var(--txt)] sm:text-6xl lg:text-7xl">
              More ways to make OpenCode yours.
            </h1>
            <p className="mt-6 max-w-xl text-base leading-7 text-[var(--muted)] sm:text-lg sm:leading-8">
              Find community-built skills, agents, commands, skill packs, and MCP servers—then inspect the source before you install.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link href="/marketplace" className="inline-flex min-h-12 items-center gap-3 rounded-md bg-[var(--accent)] px-5 text-sm font-bold text-[var(--accent-foreground)] transition-transform hover:-translate-y-0.5">
                Explore the directory <span aria-hidden="true">↗</span>
              </Link>
              <a href="https://github.com/kabirprokk/opencode-marketplace" target="_blank" rel="noopener noreferrer" className="inline-flex min-h-12 items-center rounded-md border border-[var(--line-strong)] px-4 text-sm font-medium text-[var(--txt)] transition-colors hover:bg-[var(--surface-overlay)]">
                Inspect the project
              </a>
            </div>
            <p className="mt-5 text-xs leading-5 text-[var(--muted)]">Community-maintained. Not affiliated with or endorsed by Anomaly.</p>
          </div>

          <div className="relative mx-auto w-full max-w-lg">
            <div className="absolute -inset-5 rounded-[2rem] border border-[var(--line)] opacity-60" aria-hidden="true" />
            <div className="relative overflow-hidden rounded-2xl border border-[var(--line-strong)] bg-[var(--surface)] p-5 shadow-2xl sm:p-7">
              <div className="flex items-center justify-between border-b border-[var(--line)] pb-4">
                <div>
                  <p className="font-mono text-[10px] uppercase tracking-[0.16em] text-[var(--muted)]">The community graph</p>
                  <p className="mt-1 text-sm font-semibold text-[var(--txt)]">One place to find your next tool</p>
                </div>
                <span className="rounded-full border border-[var(--line)] px-2.5 py-1 font-mono text-[10px] text-[var(--muted)]">OPEN INDEX</span>
              </div>
              <svg className="mt-4 h-auto w-full" viewBox="0 0 440 250" role="group" aria-labelledby="graph-title">
                <title id="graph-title">Browse community packages by type</title>
                <g fill="none" stroke="var(--line-strong)" strokeWidth="1.5">
                  <path d="M220 122 104 65M220 122 334 58M220 122 94 184M220 122 344 190" />
                  <path d="M104 65h-34M334 58h35M94 184h-32M344 190h35" strokeDasharray="3 6" />
                </g>
                <g fill="var(--surface-overlay)" stroke="var(--line-strong)" strokeWidth="1.5">
                  <rect x="165" y="86" width="110" height="72" rx="12" fill="var(--bg)" />
                </g>
                <g fill="var(--txt)" fontFamily="ui-monospace, monospace" fontSize="11" fontWeight="600" textAnchor="middle">
                  <text x="220" y="119" fontSize="10" fill="var(--muted)">COMMUNITY</text>
                  <text x="220" y="137" fontSize="16" fontWeight="700">OCM</text>
                </g>
                <Link href="/opencode-skills" aria-label="Browse all skills" tabIndex={0} className="group cursor-pointer focus-visible:outline-none">
                  <rect x="27" y="38" width="112" height="52" rx="8" fill="var(--surface-overlay)" stroke="var(--line-strong)" strokeWidth="1.5" className="transition-colors group-hover:stroke-[var(--accent)] group-focus-visible:stroke-[var(--accent)]" />
                  <text x="83" y="68" fill="var(--txt)" fontFamily="ui-monospace, monospace" fontSize="11" fontWeight="600" textAnchor="middle">SKILLS</text>
                </Link>
                <Link href="/opencode-agents" aria-label="Browse all agents" tabIndex={0} className="group cursor-pointer focus-visible:outline-none">
                  <rect x="301" y="32" width="112" height="52" rx="8" fill="var(--surface-overlay)" stroke="var(--line-strong)" strokeWidth="1.5" className="transition-colors group-hover:stroke-[var(--accent)] group-focus-visible:stroke-[var(--accent)]" />
                  <text x="357" y="62" fill="var(--txt)" fontFamily="ui-monospace, monospace" fontSize="11" fontWeight="600" textAnchor="middle">AGENTS</text>
                </Link>
                <Link href="/opencode-commands" aria-label="Browse all commands" tabIndex={0} className="group cursor-pointer focus-visible:outline-none">
                  <rect x="22" y="158" width="112" height="52" rx="8" fill="var(--surface-overlay)" stroke="var(--line-strong)" strokeWidth="1.5" className="transition-colors group-hover:stroke-[var(--accent)] group-focus-visible:stroke-[var(--accent)]" />
                  <text x="78" y="188" fill="var(--txt)" fontFamily="ui-monospace, monospace" fontSize="11" fontWeight="600" textAnchor="middle">COMMANDS</text>
                </Link>
                <Link href="/opencode-mcp-servers" aria-label="Browse all MCP servers" tabIndex={0} className="group cursor-pointer focus-visible:outline-none">
                  <rect x="306" y="164" width="112" height="52" rx="8" fill="var(--surface-overlay)" stroke="var(--line-strong)" strokeWidth="1.5" className="transition-colors group-hover:stroke-[var(--accent)] group-focus-visible:stroke-[var(--accent)]" />
                  <text x="362" y="194" fill="var(--txt)" fontFamily="ui-monospace, monospace" fontSize="11" fontWeight="600" textAnchor="middle">MCP SERVERS</text>
                </Link>
              </svg>
              <div className="flex items-center justify-between border-t border-[var(--line)] pt-4 text-xs text-[var(--muted)]">
                <span>{packages.length.toLocaleString()} listed packages</span>
                <a href="https://github.com/kabirprokk/opencode-registry" target="_blank" rel="noopener noreferrer" className="font-medium text-[var(--accent-text)] hover:underline">View registry source ↗</a>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className="mx-auto grid max-w-7xl grid-cols-2 gap-px border-b border-[var(--line)] bg-[var(--line)] px-5 sm:px-6 md:grid-cols-4">
        {[
          { value: packages.length.toLocaleString(), label: "community packages" },
          { value: packageTypes.length.toString().padStart(2, "0"), label: "package types" },
          { value: "Open", label: "source registry" },
          { value: "Independent", label: "community project" },
        ].map((item) => (
          <div key={item.label} className="bg-[var(--bg)] px-3 py-5 sm:px-5 sm:py-6">
            <p className="font-serif text-xl text-[var(--txt)] sm:text-2xl">{item.value}</p>
            <p className="mt-1 font-mono text-[9px] uppercase tracking-[0.12em] text-[var(--muted)] sm:text-[10px]">{item.label}</p>
          </div>
        ))}
      </div>

      <section id="finder" className="mx-auto max-w-7xl px-5 pb-12 pt-12 sm:px-6 sm:pb-16 sm:pt-16">
      <div className="rounded-xl border border-[var(--line)] bg-[var(--surface)] p-5 sm:p-8">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="mb-2 text-xs font-medium uppercase tracking-[0.12em] text-[var(--muted)]">Quick match · no typing</p>
            <h2 className="text-2xl font-semibold tracking-tight text-[var(--txt)] sm:text-3xl">What do you want help with?</h2>
            <p className="mt-2 text-sm text-[var(--muted)]">Pick up to three cards. We’ll match them to packages in the catalog.</p>
          </div>
          {selectedTopics.length > 0 && (
            <button type="button" onClick={reset} className="min-h-10 rounded-md px-3 text-sm font-medium text-[var(--muted)] transition-colors hover:bg-[var(--surface-overlay)] hover:text-[var(--txt)]">
              Clear choices
            </button>
          )}
        </div>

        <fieldset className="mt-6">
          <legend className="sr-only">Choose up to three work areas</legend>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-2 sm:gap-3 lg:grid-cols-4">
            {TOPICS.map((topic) => {
              const selected = selectedTopics.includes(topic.id);
              const disabled = !selected && selectedTopics.length >= 3;
              return (
                <button
                  key={topic.id}
                  type="button"
                  aria-pressed={selected}
                  disabled={disabled}
                  onClick={() => toggleTopic(topic.id)}
                  className={`relative flex min-h-24 flex-col items-start justify-center rounded-lg border p-3 text-left transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--accent)] sm:p-4 ${selected ? "border-[var(--accent)] bg-[var(--surface-overlay)]" : "border-[var(--line)] hover:border-[var(--line-strong)] hover:bg-[var(--surface-overlay)]"} ${disabled ? "cursor-not-allowed opacity-45" : ""}`}
                >
                  <span className="flex w-full items-center justify-between gap-2 text-sm font-semibold text-[var(--txt)]">
                    {topic.title}
                    {selected && <CheckIcon className="h-4 w-4 shrink-0 text-[var(--accent-text)]" />}
                  </span>
                  <span className="mt-1 text-xs leading-5 text-[var(--muted)]">{topic.detail}</span>
                </button>
              );
            })}
          </div>
        </fieldset>

        <fieldset className="mt-6 border-t border-[var(--line)] pt-5">
          <legend className="text-sm font-medium text-[var(--txt)]">Prefer a package type? <span className="font-normal text-[var(--muted)]">Optional</span></legend>
          <div className="mt-3 flex flex-wrap gap-2">
            <button
              type="button"
              aria-pressed={selectedType === "all"}
              onClick={() => { setSelectedType("all"); setSubmitted(false); }}
              className={`min-h-10 rounded-md border px-3 text-sm transition-colors ${selectedType === "all" ? "border-[var(--accent)] bg-[var(--surface-overlay)] text-[var(--txt)]" : "border-[var(--line)] text-[var(--muted)] hover:bg-[var(--surface-overlay)]"}`}
            >
              Any type
            </button>
            {packageTypes.map((type) => (
              <button
                key={type}
                type="button"
                aria-pressed={selectedType === type}
                onClick={() => { setSelectedType(type); setSubmitted(false); }}
                className={`min-h-10 rounded-md border px-3 text-sm transition-colors ${selectedType === type ? "border-[var(--accent)] bg-[var(--surface-overlay)] text-[var(--txt)]" : "border-[var(--line)] text-[var(--muted)] hover:bg-[var(--surface-overlay)]"}`}
              >
                {TYPE_LABELS[type] ?? type}
              </button>
            ))}
          </div>
        </fieldset>

        <div className="mt-6 flex flex-wrap items-center gap-3 border-t border-[var(--line)] pt-5">
          <button
            type="button"
            disabled={selectedTopics.length === 0}
            onClick={() => setSubmitted(true)}
            className="inline-flex min-h-11 items-center rounded-md bg-[var(--accent)] px-4 text-sm font-semibold text-[var(--accent-foreground)] transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-45"
          >
            Show my matches
          </button>
          <Link href="/marketplace" className="inline-flex min-h-11 items-center rounded-md px-3 text-sm font-medium text-[var(--muted)] transition-colors hover:bg-[var(--surface-overlay)] hover:text-[var(--txt)]">
            Browse all packages instead
          </Link>
          <p className="text-xs text-[var(--muted)]">Your choices stay in this page; nothing is sent or saved.</p>
        </div>
      </div>

      {submitted && (
        <>
          <section aria-labelledby="matches-title" className="mt-10 scroll-mt-8">
            <div className="flex flex-wrap items-end justify-between gap-3 border-b border-[var(--line)] pb-4">
              <div>
                <p className="mb-2 text-xs font-medium uppercase tracking-[0.12em] text-[var(--muted)]">Your shortlist</p>
                <h2 id="matches-title" className="text-2xl font-semibold tracking-tight text-[var(--txt)]">{matches.length ? "A good place to start" : "No exact matches yet"}</h2>
              </div>
              <p className="text-sm text-[var(--muted)]" role="status" aria-live="polite">{matches.length} matching {matches.length === 1 ? "package" : "packages"}</p>
            </div>
            {matches.length ? (
              <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
                {matches.slice(0, 6).map((item) => (
                  <div key={item.pkg.name}>
                    <p className="mb-2 text-xs text-[var(--muted)]">Matched for {item.reasons.join(" · ")}</p>
                    <PackageCard pkg={item.pkg} />
                  </div>
                ))}
              </div>
            ) : (
              <p className="mt-5 rounded-lg border border-[var(--line)] bg-[var(--surface)] p-5 text-sm leading-6 text-[var(--muted)]">
                Try another card or package type, or browse the full catalog to explore everything.
              </p>
            )}
            <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
              {matches.length > 6 && <p className="text-sm text-[var(--muted)]">Showing the top 6 of {matches.length} matches.</p>}
              <Link href="/marketplace" className="ml-auto inline-flex min-h-10 items-center rounded-md border border-[var(--line-strong)] px-3 text-sm font-medium text-[var(--txt)] transition-colors hover:bg-[var(--surface-overlay)]">
                Explore all {packages.length} packages
              </Link>
            </div>
          </section>

          <section id="skills" className="mx-auto mt-16 max-w-7xl scroll-mt-8 border-t border-[var(--line)] px-0 pt-10">
            <div className="flex flex-wrap items-end justify-between gap-4 border-b border-[var(--line)] pb-5">
              <div>
                <p className="mb-2 text-xs font-medium uppercase tracking-[0.12em] text-[var(--muted)]">Explore the collection</p>
                <h2 className="text-2xl font-semibold tracking-tight text-[var(--txt)] sm:text-3xl">Community skills</h2>
              </div>
              <p className="text-sm text-[var(--muted)]">{skills.length} skills · A–Z</p>
            </div>
            {skills.length ? (
              <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
                {skills.map((pkg) => <PackageCard key={pkg.name} pkg={pkg} showType={false} />)}
              </div>
            ) : (
              <p className="mt-6 rounded-lg border border-[var(--line)] bg-[var(--surface)] p-6 text-sm text-[var(--muted)]">No skills are listed yet.</p>
            )}
            <div className="mt-8 flex justify-center">
              <Link href="/marketplace" className="inline-flex min-h-11 items-center rounded-md border border-[var(--line-strong)] px-4 text-sm font-medium text-[var(--txt)] transition-colors hover:bg-[var(--surface-overlay)]">
                Browse all package types
              </Link>
            </div>
          </section>
        </>
      )}
      </section>

      <section id="maintainers" className="mx-auto max-w-7xl px-5 pb-16 sm:px-6 sm:pb-24">
        <div className="grid gap-6 rounded-2xl border border-[var(--line)] bg-[var(--surface)] p-6 sm:p-9 lg:grid-cols-[1fr_auto] lg:items-center">
          <div className="max-w-2xl">
            <p className="font-mono text-[10px] font-semibold uppercase tracking-[0.18em] text-[var(--accent-text)]">For the OpenCode maintainers</p>
            <h2 className="mt-3 font-serif text-3xl tracking-tight text-[var(--txt)] sm:text-4xl">Built by the community. Open to your review.</h2>
            <p className="mt-3 max-w-xl text-sm leading-6 text-[var(--muted)]">OCM is an independent directory with a public registry and CLI. If this could be useful to the OpenCode ecosystem, we welcome feedback and a conversation about where it belongs.</p>
            <p className="mt-3 text-xs leading-5 text-[var(--muted)]">OCM is not affiliated with or endorsed by Anomaly. Package sources and permissions should be reviewed before installation.</p>
          </div>
          <div className="flex flex-wrap gap-3 lg:justify-end">
            <a href="https://github.com/kabirprokk/opencode-marketplace" target="_blank" rel="noopener noreferrer" className="inline-flex min-h-11 items-center rounded-md bg-[var(--accent)] px-4 text-sm font-semibold text-[var(--accent-foreground)] hover:opacity-90">Review OCM on GitHub ↗</a>
            <a href="https://github.com/kabirprokk/opencode-registry" target="_blank" rel="noopener noreferrer" className="inline-flex min-h-11 items-center rounded-md border border-[var(--line-strong)] px-4 text-sm font-medium text-[var(--txt)] hover:bg-[var(--surface-overlay)]">Inspect registry ↗</a>
          </div>
        </div>
      </section>
    </>
  );
}
