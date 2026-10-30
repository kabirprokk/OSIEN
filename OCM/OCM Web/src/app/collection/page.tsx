import type { Metadata } from "next";
import Link from "next/link";
import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";
import { PackageCard } from "@/components/PackageCard";
import { AgentPrompt } from "@/components/AgentPrompt";
import { fetchRegistry } from "@/lib/registry";

export const metadata: Metadata = {
  title: "Collection — All OpenCode packages sorted",
  description:
    "The full collection of OpenCode skills, agents, commands, skill packs, and MCP servers, sorted and grouped by type.",
  alternates: { canonical: "/collection/" },
};

const TYPE_LABELS: Record<string, string> = {
  skill: "Skills",
  agent: "Agents",
  command: "Commands",
  "skill-pack": "Skill packs",
  mcp: "MCP servers",
};

const TYPE_ORDER = ["skill", "agent", "command", "skill-pack", "mcp"];

export default async function CollectionPage() {
  const registry = await fetchRegistry();
  const packages = [...registry.packages].sort((a, b) => a.name.localeCompare(b.name));

  const grouped = TYPE_ORDER.map((type) => ({
    type,
    label: TYPE_LABELS[type],
    items: packages.filter((p) => p.type === type),
  })).filter((g) => g.items.length > 0);

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <AgentPrompt packages={registry.packages} updated={registry.updated} />
      <main className="mx-auto w-full max-w-7xl flex-1 px-5 py-10 sm:px-6 sm:py-14">
        <nav aria-label="Breadcrumb" className="mb-7 text-sm text-[var(--muted)]">
          <Link href="/" className="hover:text-[var(--txt)]">Home</Link>
          <span aria-hidden="true"> / </span>
          <Link href="/marketplace" className="hover:text-[var(--txt)]">Marketplace</Link>
          <span aria-hidden="true"> / </span>
          <span aria-current="page" className="text-[var(--txt)]">Collection</span>
        </nav>

        <header className="max-w-3xl border-b border-[var(--line)] pb-8">
          <p className="mb-3 font-mono text-[10px] font-semibold uppercase tracking-[0.18em] text-[var(--accent-text)]">
            Full collection · {packages.length} packages
          </p>
          <h1 className="font-serif text-4xl font-medium tracking-tight text-[var(--txt)] sm:text-5xl">
            All resources, sorted
          </h1>
          <p className="mt-5 text-base leading-7 text-[var(--muted)]">
            Every skill, agent, command, skill pack, and MCP server in the community index — sorted alphabetically, grouped by type.
          </p>
        </header>

        <section aria-labelledby="all-sorted" className="py-9">
          <h2 id="all-sorted" className="mb-5 text-2xl font-semibold tracking-tight text-[var(--txt)]">
            All (sorted)
          </h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {packages.map((pkg) => (
              <PackageCard key={pkg.name} pkg={pkg} />
            ))}
          </div>
        </section>

        {grouped.map((group) => (
          <section key={group.type} aria-labelledby={`${group.type}-group`} className="border-t border-[var(--line)] py-9">
            <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
              <h2 id={`${group.type}-group`} className="text-2xl font-semibold tracking-tight text-[var(--txt)]">
                {group.label}
              </h2>
              <p className="text-sm text-[var(--muted)]">{group.items.length} items</p>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {group.items.map((pkg) => (
                <PackageCard key={pkg.name} pkg={pkg} showType={false} />
              ))}
            </div>
          </section>
        ))}
      </main>
      <Footer />
    </div>
  );
}