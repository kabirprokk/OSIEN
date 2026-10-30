import Link from "next/link";
import { SEO_CATEGORIES } from "@/lib/seo-categories";

export function CategoryLinks({ currentSlug }: { currentSlug?: string }) {
  return (
    <nav aria-label="Browse OpenCode package categories" className="flex flex-wrap gap-2">
      {SEO_CATEGORIES.filter((category) => category.slug !== currentSlug).map((category) => (
        <Link
          key={category.slug}
          href={`/${category.slug}`}
          className="inline-flex min-h-10 items-center rounded-md border border-[var(--line)] px-3 text-sm text-[var(--muted)] transition-colors hover:border-[var(--line-strong)] hover:bg-[var(--surface-overlay)] hover:text-[var(--txt)]"
        >
          {category.label}
        </Link>
      ))}
    </nav>
  );
}
