import { CategoryLinks } from "@/components/CategoryLinks";

export function Footer() {
  return (
    <footer className="border-t border-[var(--line)]">
      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-5 py-6 text-xs text-[var(--muted)] sm:px-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p>Independent community directory · Not affiliated with Anomaly.</p>
          <div className="flex items-center gap-4">
            <a href="https://github.com/kabirprokk/opencode-registry" className="hover:text-[var(--txt)]">Registry</a>
            <a href="https://github.com/kabirprokk/opencode-marketplace" className="hover:text-[var(--txt)]">CLI source</a>
          </div>
        </div>
        <CategoryLinks />
      </div>
    </footer>
  );
}
