import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { CategoryLinks } from "@/components/CategoryLinks";
import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";
import { PackageCard } from "@/components/PackageCard";
import { StructuredData } from "@/components/StructuredData";
import { fetchRegistry } from "@/lib/registry";
import { getSeoCategory, SEO_CATEGORIES } from "@/lib/seo-categories";

export const dynamicParams = false;

export function generateStaticParams() {
  return SEO_CATEGORIES.map(({ slug }) => ({ category: slug }));
}

type PageProps = { params: Promise<{ category: string }> };

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { category: slug } = await params;
  const category = getSeoCategory(slug);
  if (!category) return { title: "Category not found", robots: { index: false, follow: false } };

  const url = `/${category.slug}`;
  return {
    title: category.title,
    description: category.description,
    keywords: [category.label, "OpenCode marketplace", "OpenCode community", "AI coding tools"],
    alternates: { canonical: url },
    openGraph: {
      title: category.title,
      description: category.description,
      url,
      siteName: "OpenCode Marketplace",
      type: "website",
      images: [{ url: "/opencode-intro-poster.jpg", width: 960, height: 540, alt: category.label }],
    },
    twitter: { card: "summary_large_image", title: category.title, description: category.description, images: ["/opencode-intro-poster.jpg"] },
  };
}

export default async function CategoryPage({ params }: PageProps) {
  const { category: slug } = await params;
  const category = getSeoCategory(slug);
  if (!category) notFound();

  const { packages } = await fetchRegistry();
  const entries = packages.filter((pkg) => pkg.type === category.type);
  const breadcrumbData = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "Home", item: "https://ocms.pages.dev/" },
      { "@type": "ListItem", position: 2, name: "Marketplace", item: "https://ocms.pages.dev/marketplace" },
      { "@type": "ListItem", position: 3, name: category.label, item: `https://ocms.pages.dev/${category.slug}` },
    ],
  };

  return (
    <div className="flex min-h-screen flex-col">
      <StructuredData data={breadcrumbData} />
      <Header />
      <main className="mx-auto w-full max-w-7xl flex-1 px-5 py-10 sm:px-6 sm:py-14">
        <nav aria-label="Breadcrumb" className="mb-7 text-sm text-[var(--muted)]">
          <Link href="/" className="hover:text-[var(--txt)]">Home</Link>
          <span aria-hidden="true"> / </span>
          <Link href="/marketplace" className="hover:text-[var(--txt)]">Marketplace</Link>
          <span aria-hidden="true"> / </span>
          <span aria-current="page" className="text-[var(--txt)]">{category.label}</span>
        </nav>

        <header className="max-w-3xl border-b border-[var(--line)] pb-8">
          <p className="mb-3 font-mono text-[10px] font-semibold uppercase tracking-[0.18em] text-[var(--accent-text)]">
            Independent community directory · {entries.length} listings
          </p>
          <h1 className="font-serif text-4xl font-medium tracking-tight text-[var(--txt)] sm:text-5xl">
            {category.heading}
          </h1>
          <p className="mt-5 text-base leading-7 text-[var(--muted)]">{category.intro}</p>
          <p className="mt-3 text-sm leading-6 text-[var(--muted)]">{category.guidance}</p>
          <p className="mt-4 text-xs leading-5 text-[var(--muted)]">
            Community submissions are independent and unreviewed. Inspect source code and setup instructions before installing or connecting anything.
          </p>
        </header>

        <section aria-labelledby="category-listings" className="py-9">
          <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
            <div>
              <h2 id="category-listings" className="text-2xl font-semibold tracking-tight text-[var(--txt)]">{category.label} directory</h2>
              <p className="mt-1 text-sm text-[var(--muted)]">Browse descriptions, tags, authors, licenses, and source links.</p>
            </div>
            <Link href={`/marketplace?type=${category.type}`} className="text-sm font-medium text-[var(--accent-text)] hover:underline">
              Open searchable directory →
            </Link>
          </div>
          {entries.length ? (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {entries.map((pkg) => <PackageCard key={pkg.name} pkg={pkg} />)}
            </div>
          ) : (
            <p className="rounded-lg border border-[var(--line)] bg-[var(--surface)] p-5 text-sm text-[var(--muted)]">
              No packages of this type are listed yet. Browse the full directory to explore other community resources.
            </p>
          )}
        </section>

        <section aria-labelledby="category-faq" className="max-w-3xl border-t border-[var(--line)] py-8">
          <h2 id="category-faq" className="text-xl font-semibold text-[var(--txt)]">About {category.label.toLowerCase()}</h2>
          <div className="mt-4 divide-y divide-[var(--line)]">
            {category.faq.map(({ question, answer }) => (
              <details key={question} className="py-4">
                <summary className="cursor-pointer text-sm font-medium text-[var(--txt)]">{question}</summary>
                <p className="mt-2 text-sm leading-6 text-[var(--muted)]">{answer}</p>
              </details>
            ))}
          </div>
        </section>

        <section aria-labelledby="more-categories" className="border-t border-[var(--line)] pt-8">
          <h2 id="more-categories" className="mb-4 text-lg font-semibold text-[var(--txt)]">Explore more OpenCode categories</h2>
          <CategoryLinks currentSlug={category.slug} />
        </section>
      </main>
      <Footer />
    </div>
  );
}
