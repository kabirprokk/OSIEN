import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";
import { CategoryLinks } from "@/components/CategoryLinks";
import { IntroVideo } from "@/components/IntroVideo";
import { PackageFinder } from "@/components/PackageFinder";
import { AgentPrompt } from "@/components/AgentPrompt";
import { Package } from "@/lib/registry";

export function LandingHome({ packages, updated }: { packages: Package[]; updated?: string }) {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <AgentPrompt packages={packages} updated={updated} />
      <main className="flex-1">
        <IntroVideo />
        <section aria-labelledby="browse-by-type" className="mx-auto max-w-7xl px-5 pb-8 sm:px-6">
          <h2 id="browse-by-type" className="mb-3 text-sm font-semibold text-[var(--txt)]">Browse OpenCode resources by type</h2>
          <CategoryLinks />
        </section>
        <PackageFinder packages={packages} />
      </main>
      <Footer />
    </div>
  );
}
