"use client";

import { useMemo, useState } from "react";
import { Package } from "@/lib/registry";
import { CheckIcon } from "@/components/TypeIcon";

export function AgentPrompt({
  packages,
  updated,
}: {
  packages: Package[];
  updated?: string;
}) {
  const [copied, setCopied] = useState(false);

  const expandedSkills = useMemo(
    () =>
      packages.filter((p) => p.type === "skill").length +
      packages
        .filter((p) => p.type === "skill-pack")
        .reduce((sum, p) => sum + (p.skill_count ?? 0), 0),
    [packages],
  );

  const prompt = useMemo(() => {
    const groups: Record<string, Package[]> = {};
    for (const pkg of packages) {
      (groups[pkg.type] ??= []).push(pkg);
    }
    const typeLabels: Record<string, string> = {
      skill: "Skills",
      agent: "Agents",
      command: "Commands",
      "skill-pack": "Skill packs",
      mcp: "MCP servers",
    };
    const lines: string[] = [];
    lines.push("You are working with OpenCode and have access to the community marketplace.");
    lines.push("New tools, skills, agents, commands, and MCP servers may have been added since your last update.");
    lines.push(`Registry updated: ${updated || "latest"}. Total packages: ${packages.length} (expands to ${expandedSkills} skills).`);
    lines.push("");
    lines.push("Use these resources when relevant. Install any of them with: ocm install <name>");
    lines.push("");
    for (const type of ["skill", "agent", "command", "skill-pack", "mcp"]) {
      const list = (groups[type] ?? []).sort((a, b) => a.name.localeCompare(b.name));
      if (!list.length) continue;
      lines.push(`## ${typeLabels[type]} (${list.length})`);
      for (const pkg of list) {
        lines.push(`- ${pkg.name}: ${pkg.description}  [ocm install ${pkg.name}]`);
      }
      lines.push("");
    }
    lines.push("When the user asks for something covered above, prefer the matching package and install it with ocm before proceeding.");
    return lines.join("\n");
  }, [packages, updated, expandedSkills]);

  const copy = async () => {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(prompt);
      } else {
        const textarea = document.createElement("textarea");
        textarea.value = prompt;
        textarea.style.position = "fixed";
        textarea.style.opacity = "0";
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand("copy");
        document.body.removeChild(textarea);
      }
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setCopied(false);
    }
  };

  return (
    <section className="mx-auto max-w-7xl px-5 py-8 sm:px-6" aria-labelledby="agent-prompt-title">
      <div className="rounded-xl border border-[var(--line)] bg-[var(--surface)] p-5 sm:p-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="font-mono text-[10px] font-semibold uppercase tracking-[0.18em] text-[var(--accent-text)]">
              Agent prompt
            </p>
            <h2 id="agent-prompt-title" className="mt-1 text-xl font-semibold tracking-tight text-[var(--txt)] sm:text-2xl">
              Updated prompt for your agent
            </h2>
            <p className="mt-1 text-sm text-[var(--muted)]">
              Copy this prompt and give it to your agent whenever new tools are added. It always reflects the latest registry.
            </p>
          </div>
          <button
            type="button"
            onClick={copy}
            className="inline-flex min-h-10 items-center gap-2 rounded-md bg-[var(--accent)] px-4 text-sm font-semibold text-[var(--accent-foreground)] transition-opacity hover:opacity-90"
          >
            {copied ? (
              <span className="inline-flex items-center gap-1">
                <CheckIcon className="h-3.5 w-3.5" />
                Copied
              </span>
            ) : (
              "Copy prompt"
            )}
          </button>
        </div>
        <textarea
          readOnly
          value={prompt}
          aria-label="Updated agent prompt"
          className="mt-4 h-48 w-full resize-y rounded-md border border-[var(--line)] bg-[var(--bg)] p-3 font-mono text-xs leading-5 text-[var(--txt)] focus:outline-none"
        />
        <p className="mt-2 text-xs text-[var(--muted)]">
          {expandedSkills} skills available across {packages.length} packages · updated {updated || "just now"}
        </p>
      </div>
    </section>
  );
}