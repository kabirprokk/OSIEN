import { LandingHome } from "@/components/LandingHome";
import { fetchRegistry } from "@/lib/registry";

export default async function HomePage() {
  const registry = await fetchRegistry();
  return <LandingHome packages={registry.packages} updated={registry.updated} />;
}
