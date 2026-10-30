"use client";

import { useState, useMemo, useEffect, useCallback } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { PackageCard } from "@/components/PackageCard";
import { AgentPrompt } from "@/components/AgentPrompt";
import { Package, PackageType } from "@/lib/registry";

const TYPES: { value: PackageType | "all"; label: string }[] = [
  { value: "all", label: "All package types" },
  { value: "skill", label: "Skills" },
  { value: "agent", label: "Agents" },
  { value: "command", label: "Commands" },
  { value: "skill-pack", label: "Skill packs" },
  { value: "mcp", label: "MCP servers" },
];

const PAGE_SIZE = 24;

export function MarketplaceHome({ packages, updated }: { packages: Package[]; updated?: string }) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const [query, setQuery] = useState(searchParams.get("q") ?? "");
  const [typeFilter, setTypeFilter] = useState<PackageType | "all">(
    (searchParams.get("type") as PackageType | "all" | null) ?? "all",
  );
  const [sortBy, setSortBy] = useState<"name" | "author">("name");
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);

  // Sync state -> URL (shareable filters)
  useEffect(() => {
    const params = new URLSearchParams(searchParams.toString());
    if (query) params.set("q", query); else params.delete("q");
    if (typeFilter !== "all") params.set("type", typeFilter); else params.delete("type");
    const next = params.toString();
    const current = searchParams.toString();
    if (next !== current) {
      router.replace(`${pathname}${next ? `?${next}` : ""}`, { scroll: false });
    }
  }, [query, typeFilter, pathname, router, searchParams]);

  const filtered = useMemo(() => {
    const q = query.toLowerCase();
    const result = packages.filter((p) => {
      if (typeFilter !== "all" && p.type !== typeFilter) return false;
      if (!q) return true;
      return (
        p.name.toLowerCase().includes(q) ||
        p.description.toLowerCase().includes(q) ||
        p.tags.some((t) => t.toLowerCase().includes(q)) ||
        p.author.toLowerCase().includes(q)
      );
    });
    result.sort((a, b) => {
      if (sortBy === "author") {
        const byAuthor = a.author.localeCompare(b.author);
        if (byAuthor !== 0) return byAuthor;
      }
      return a.name.localeCompare(b.name);
    });
    return result;
  }, [packages, query, typeFilter, sortBy]);

  // Pagination resets are handled in the change handlers below to avoid
  // calling setState synchronously inside an effect.

  const visible = typeFilter === "all" ? filtered.slice(0, visibleCount) : filtered;
  const hasMore = typeFilter === "all" && visibleCount < filtered.length;

  const clearFilters = useCallback(() => {
    setQuery("");
    setTypeFilter("all");
    setSortBy("name");
  }, []);

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <AgentPrompt packages={packages} updated={updated} />
      <main className="flex-1">
        <section id="packages" className="mx-auto max-w-7xl px-5 pb-16 pt-10 sm:px-6 sm:pb-20 sm:pt-14">
          <div className="border-b border-[var(--line)] pb-6">
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div>
                <p className="mb-3 font-mono text-[10px] font-semibold uppercase tracking-[0.18em] text-[var(--accent-text)]">Independent community index</p>
                <h1 className="font-serif text-4xl font-medium tracking-tight text-[var(--txt)] sm:text-5xl">
                  The directory
                </h1>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--muted)]">
                  Explore skills, agents, commands, skill packs, and MCP servers shared by the community. Inspect each source before installing.
                </p>
              </div>
              <a href="https://github.com/kabirprokk/opencode-registry" target="_blank" rel="noopener noreferrer" className="inline-flex min-h-10 items-center gap-2 rounded-md border border-[var(--line)] px-3 text-xs font-medium text-[var(--muted)] transition-colors hover:bg-[var(--surface-overlay)] hover:text-[var(--txt)]">
                Registry source <span aria-hidden="true">↗</span>
              </a>
            </div>

            <div className="mt-6 grid gap-3 sm:grid-cols-[minmax(0,1fr)_160px_160px]">
              <div className="relative block">
                <input
                  type="search"
                  aria-label="Search packages by name, description, tag, or author"
                  placeholder="Search packages"
                  value={query}
                  onChange={(e) => { setQuery(e.target.value); setVisibleCount(PAGE_SIZE); }}
                  className="min-h-11 w-full rounded-md border border-[var(--line)] bg-[var(--surface)] py-2.5 pl-4 pr-10 text-sm text-[var(--txt)] placeholder-[var(--muted)] transition-colors focus:border-[var(--accent)] focus:outline-none focus:ring-1 focus:ring-[var(--accent)]"
                />
              </div>
              <div>
                <select
                  aria-label="Filter by package type"
                  value={typeFilter}
                  onChange={(e) => { setTypeFilter(e.target.value as PackageType | "all"); setVisibleCount(PAGE_SIZE); }}
                  className="min-h-11 w-full rounded-md border border-[var(--line)] bg-[var(--surface)] px-3 text-sm text-[var(--txt)] focus:border-[var(--accent)] focus:outline-none focus:ring-1 focus:ring-[var(--accent)]"
                >
                  {TYPES.map((type) => (
                    <option key={type.value} value={type.value}>{type.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <select
                  aria-label="Sort packages"
                  value={sortBy}
                  onChange={(e) => { setSortBy(e.target.value as "name" | "author"); setVisibleCount(PAGE_SIZE); }}
                  className="min-h-11 w-full rounded-md border border-[var(--line)] bg-[var(--surface)] px-3 text-sm text-[var(--txt)] focus:border-[var(--accent)] focus:outline-none focus:ring-1 focus:ring-[var(--accent)]"
                >
                  <option value="name">Sort: Name</option>
                  <option value="author">Sort: Author</option>
                </select>
              </div>
            </div>

            <div className="mt-4 flex min-h-5 items-center justify-between gap-3 text-xs text-[var(--muted)]">
              <p role="status" aria-live="polite">
                {filtered.length === packages.length
                  ? `${packages.length} packages`
                  : `${filtered.length} of ${packages.length} packages`}
                {hasMore ? ` · showing ${visible.length}` : ""}
              </p>
              {(query || typeFilter !== "all" || sortBy !== "name") && (
                <button
                  type="button"
                  onClick={clearFilters}
                  className="min-h-8 rounded-md px-2 text-xs font-medium text-[var(--accent-text)] hover:bg-[var(--surface-overlay)]"
                >
                  Clear filters
                </button>
              )}
            </div>
          </div>

          <div className="pt-6">
            {filtered.length === 0 ? (
              <div className="rounded-lg border border-[var(--line)] bg-[var(--surface)] px-5 py-14 text-center">
                <p className="text-sm font-medium text-[var(--txt)]">No packages found</p>
                <p className="mt-1 text-sm text-[var(--muted)]">Try another search or package type.</p>
                <button
                  onClick={clearFilters}
                  type="button"
                  className="mt-4 min-h-10 rounded-md px-3 text-sm font-medium text-[var(--accent-text)] hover:bg-[var(--surface-overlay)]"
                >
                  Clear search
                </button>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
                  {visible.map((pkg) => (
                    <PackageCard key={pkg.name} pkg={pkg} />
                  ))}
                </div>
                {hasMore && (
                  <div className="mt-8 flex justify-center">
                    <button
                      type="button"
                      onClick={() => setVisibleCount((c) => c + PAGE_SIZE)}
                      className="min-h-11 rounded-md border border-[var(--line-strong)] px-5 text-sm font-medium text-[var(--txt)] transition-colors hover:bg-[var(--surface-overlay)]"
                    >
                      Load more ({filtered.length - visible.length} remaining)
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        </section>
      </main>
      <Footer />
    </div>
  );
}
