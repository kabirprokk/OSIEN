"use client";

import { useState } from "react";
import Link from "next/link";
import { Package } from "@/lib/registry";
import { CheckIcon } from "@/components/TypeIcon";

export function PackageCard({ pkg, showType = true }: { pkg: Package; showType?: boolean }) {
  const [copied, setCopied] = useState(false);
  const installCmd = `ocm install ${pkg.name}`;
  const sourceHref = /^https?:\/\//i.test(pkg.repo) ? pkg.repo : null;
  const PackageHeading = showType ? "h2" : "h3";

  const copy = async () => {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(installCmd);
      } else {
        const textarea = document.createElement("textarea");
        textarea.value = installCmd;
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
    <article className="group flex min-h-56 flex-col rounded-lg border border-[var(--line)] bg-[var(--surface)] p-5 transition-colors hover:border-[var(--line-strong)]">
      {showType && (
        <div className="mb-2">
          <span className="text-[11px] font-medium uppercase tracking-[0.12em] text-[var(--muted)]">
            {pkg.type.replace("-", " ")}
          </span>
        </div>
      )}
      <PackageHeading className="truncate text-base font-semibold tracking-tight text-[var(--txt)] transition-colors group-hover:text-[var(--accent-text)]">
        <Link href={`/packages/${encodeURIComponent(pkg.name)}`} aria-label={`View ${pkg.name} package details`}>
          {pkg.name}
        </Link>
      </PackageHeading>
      <p className="mt-2 line-clamp-3 flex-1 text-sm leading-6 text-[var(--muted)]">
        {pkg.description}
      </p>
      <p className="mt-3 text-xs text-[var(--muted)]">by {pkg.author} <span aria-hidden="true">·</span> {pkg.license}</p>
      {pkg.tags.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {pkg.tags.slice(0, 4).map((tag) => (
            <span key={tag} className="rounded-full border border-[var(--line)] px-2 py-0.5 text-[10px] text-[var(--muted)]">
              {tag}
            </span>
          ))}
          {pkg.tags.length > 4 && (
            <span className="text-[10px] text-[var(--muted)]">+{pkg.tags.length - 4}</span>
          )}
        </div>
      )}
      {sourceHref && (
        <a
          href={sourceHref}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-3 inline-flex min-h-9 w-fit items-center gap-1 text-xs font-medium text-[var(--accent-text)] underline-offset-4 hover:underline"
          aria-label={`Inspect source repository for ${pkg.name}`}
        >
          Inspect source <span aria-hidden="true">↗</span>
        </a>
      )}
      <button
        onClick={copy}
        type="button"
        aria-label={`Copy install command: ${installCmd}`}
        className="mt-4 flex min-h-11 w-full items-center justify-between gap-3 rounded-md bg-[var(--surface-overlay)] px-3 text-left transition-colors hover:bg-[var(--accent)] hover:text-[var(--accent-foreground)]"
      >
        <code className="min-w-0 truncate font-mono text-xs">{installCmd}</code>
        <span className="shrink-0 text-xs font-medium">
          {copied ? <span className="inline-flex items-center gap-1"><CheckIcon className="h-3 w-3" />Copied</span> : "Copy"}
        </span>
      </button>
    </article>
  );
}
