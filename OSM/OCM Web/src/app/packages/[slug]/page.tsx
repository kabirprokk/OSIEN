import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Footer } from "@/components/Footer";
import { Header } from "@/components/Header";
import { PackageCard } from "@/components/PackageCard";
import { StructuredData } from "@/components/StructuredData";
import { fetchRegistry, type Package, type PackageType } from "@/lib/registry";
import { getSeoCategoryForType } from "@/lib/seo-categories";

export const dynamicParams = false;

type PageProps = { params: Promise<{ slug: string }> };

function packageUrl(name: string) {
  return `/packages/${encodeURIComponent(name)}`;
}

function categoryUrl(type: PackageType) {
  return `/${getSeoCategoryForType(type)?.slug ?? "marketplace"}`;
}

async function getPackage(slug: string): Promise<{ pkg: Package; packages: Package[] } | null> {
  const registry = await fetchRegistry();
  const pkg = registry.packages.find((entry) => entry.name === slug);
  return pkg ? { pkg, packages: registry.packages } : null;
}

export async function generateStaticParams() {
  const { packages } = await fetchRegistry();
  return packages.map(({ name }) => ({ slug: name }));
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  const result = await getPackage(slug);
  if (!result) return { title: "Package not found", robots: { index: false, follow: false } };

  const { pkg } = result;
  const category = getSeoCategoryForType(pkg.type);
  const kind = category?.label ?? "OpenCode package";
  const description = pkg.description.length > 155
    ? `${pkg.description.slice(0, 152).replace(/\s+\S*$/, "")}…`
    : pkg.description;
  const title = `${pkg.name} — ${kind}`;
  const url = packageUrl(pkg.name);

  return {
    title,
    description,
    keywords: [pkg.name, kind, "OpenCode marketplace", ...pkg.tags.slice(0, 5)],
    alternates: { canonical: url },
    openGraph: {
      title,
      description,
      url,
      siteName: "OpenCode Marketplace",
      type: "website",
      images: [{ url: "/opencode-intro-poster.jpg", width: 960, height: 540, alt: pkg.name }],
    },
    twitter: { card: "summary_large_image", title, description, images: ["/opencode-intro-poster.jpg"] },
  };
}

export default async function PackageDetailPage({ params }: PageProps) {
  const { slug } = await params;
  const result = await getPackage(slug);
  if (!result) notFound();

  const { pkg, packages } = result;
  const category = getSeoCategoryForType(pkg.type);
  const related = packages.filter((entry) => entry.type === pkg.type && entry.name !== pkg.name).slice(0, 3);
  const url = `https://ocms.pages.dev${packageUrl(pkg.name)}`;
  const supportsOcmInstall = pkg.type !== "mcp";
  const breadcrumbData = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "Home", item: "https://ocms.pages.dev/" },
      { "@type": "ListItem", position: 2, name: "Marketplace", item: "https://ocms.pages.dev/marketplace" },
      ...(category ? [{ "@type": "ListItem", position: 3, name: category.label, item: `https://ocms.pages.dev/${category.slug}` }] : []),
      { "@type": "ListItem", position: category ? 4 : 3, name: pkg.name, item: url },
    ],
  };

  return (
    <div className="flex min-h-screen flex-col">
      <StructuredData data={breadcrumbData} />
      <Header />
      <main className="mx-auto w-full max-w-5xl flex-1 px-5 py-10 sm:px-6 sm:py-14">
        <nav aria-label="Breadcrumb" className="mb-8 text-sm text-[var(--muted)]">
          <Link href="/" className="hover:text-[var(--txt)]">Home</Link>
          <span aria-hidden="true"> / </span>
          <Link href="/marketplace" className="hover:text-[var(--txt)]">Marketplace</Link>
          {category && <><span aria-hidden="true"> / </span><Link href={categoryUrl(pkg.type)} className="hover:text-[var(--txt)]">{category.label}</Link></>}
          <span aria-hidden="true"> / </span>
          <span aria-current="page" className="text-[var(--txt)]">{pkg.name}</span>
        </nav>

        <article>
          <header className="border-b border-[var(--line)] pb-8">
            <p className="mb-3 font-mono text-[10px] font-semibold uppercase tracking-[0.18em] text-[var(--accent-text)]">
              {category?.label ?? pkg.type.replace("-", " ")} · Community package
            </p>
            <h1 className="break-words font-serif text-4xl font-medium tracking-tight text-[var(--txt)] sm:text-5xl">{pkg.name}</h1>
            <p className="mt-5 max-w-3xl text-base leading-7 text-[var(--muted)]">{pkg.description}</p>
            <dl className="mt-6 grid grid-cols-2 gap-4 border-t border-[var(--line)] pt-5 text-sm sm:grid-cols-4">
              <div><dt className="text-xs text-[var(--muted)]">Type</dt><dd className="mt-1 text-[var(--txt)]">{pkg.type.replace("-", " ")}</dd></div>
              <div><dt className="text-xs text-[var(--muted)]">Author</dt><dd className="mt-1 break-words text-[var(--txt)]">{pkg.author}</dd></div>
              <div><dt className="text-xs text-[var(--muted)]">Version</dt><dd className="mt-1 text-[var(--txt)]">{pkg.version}</dd></div>
              <div><dt className="text-xs text-[var(--muted)]">License</dt><dd className="mt-1 text-[var(--txt)]">{pkg.license}</dd></div>
            </dl>
            {pkg.tags.length > 0 && (
              <ul aria-label="Package topics" className="mt-5 flex flex-wrap gap-2">
                {pkg.tags.map((tag) => <li key={tag} className="rounded-full border border-[var(--line)] px-2.5 py-1 text-xs text-[var(--muted)]">{tag}</li>)}
              </ul>
            )}
          </header>

          <section aria-labelledby="install-heading" className="grid gap-6 border-b border-[var(--line)] py-8 sm:grid-cols-2">
            <div>
              <h2 id="install-heading" className="text-xl font-semibold text-[var(--txt)]">Installation and setup</h2>
              {supportsOcmInstall ? (
                <>
                  <p className="mt-2 text-sm leading-6 text-[var(--muted)]">Install this package with the OpenCode Community Marketplace CLI:</p>
                  <code className="mt-4 block overflow-x-auto rounded-md border border-[var(--line)] bg-[var(--surface)] p-4 font-mono text-sm text-[var(--txt)]">ocm install {pkg.name}</code>
                </>
              ) : (
                <p className="mt-2 text-sm leading-6 text-[var(--muted)]">This package needs its own setup; follow the instructions in its source repository rather than using `ocm install`.</p>
              )}
            </div>
            <div>
              <h2 className="text-xl font-semibold text-[var(--txt)]">Review the source</h2>
              <p className="mt-2 text-sm leading-6 text-[var(--muted)]">OCM is an independent community directory. Listings are not security-audited or endorsed. Review the source, requested permissions, and license before installing or connecting this package.</p>
              <a href={pkg.repo} target="_blank" rel="noopener noreferrer" className="mt-4 inline-flex min-h-10 items-center rounded-md bg-[var(--accent)] px-4 text-sm font-semibold text-[var(--accent-foreground)] hover:opacity-90">Open source repository ↗</a>
            </div>
          </section>
        </article>

        {related.length > 0 && (
          <section aria-labelledby="related-packages" className="pt-8">
            <h2 id="related-packages" className="mb-4 text-xl font-semibold text-[var(--txt)]">More {category?.label.toLowerCase() ?? "packages"}</h2>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {related.map((entry) => <PackageCard key={entry.name} pkg={entry} />)}
            </div>
          </section>
        )}
      </main>
      <Footer />
    </div>
  );
}
