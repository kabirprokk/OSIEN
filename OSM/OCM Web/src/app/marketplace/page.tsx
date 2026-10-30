import { Suspense } from "react";
import { MarketplaceHome } from "@/components/MarketplaceHome";
import { fetchRegistry } from "@/lib/registry";

export const metadata = {
  title: "OpenCode Skills, Agents & MCP Tools Directory",
  description:
    "Search the community directory of OpenCode skills, coding agents, commands, skill packs, and MCP servers. Review package details and source links before installing.",
  alternates: {
    canonical: "/marketplace/",
  },
  openGraph: {
    title: "OpenCode Skills, Agents & MCP Tools Directory",
    description:
      "Browse and search community-built extensions for OpenCode, with source links for review.",
    url: "/marketplace/",
    type: "website" as const,
  },
};

export default async function MarketplacePage() {
  const registry = await fetchRegistry();
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <MarketplaceHome packages={registry.packages} updated={registry.updated} />
    </Suspense>
  );
}
