import type { MetadataRoute } from "next";
import { fetchRegistry } from "@/lib/registry";
import { SEO_CATEGORIES } from "@/lib/seo-categories";

export const dynamic = "force-static";

const siteUrl = "https://ocms.pages.dev";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const registry = await fetchRegistry();
  const lastModified = registry.updated ? new Date(registry.updated) : undefined;
  return [
    {
      url: `${siteUrl}/`,
      changeFrequency: "weekly",
      priority: 1,
    },
    {
      url: `${siteUrl}/marketplace/`,
      changeFrequency: "daily",
      priority: 0.9,
    },
    ...SEO_CATEGORIES.map((category) => ({
      url: `${siteUrl}/${category.slug}`,
      lastModified,
      changeFrequency: "weekly" as const,
      priority: 0.8,
    })),
    ...registry.packages.map((pkg) => ({
      url: `${siteUrl}/packages/${encodeURIComponent(pkg.name)}`,
      lastModified,
      changeFrequency: "weekly" as const,
      priority: 0.6,
    })),
  ];
}
