# OCM trust-first website and identity redesign

## Intent

Redesign OCM to earn the OpenCode maintainers' respect and make a credible,
low-pressure case for official visibility. OCM remains clearly identified as an
independent community directory; the site must not imply endorsement or
official status.

## Goals

- Give OCM a recognizable, scalable logo and visual identity that feels native
  to a developer ecosystem without copying OpenCode branding.
- Explain the user problem, what OCM offers, how to explore or install packages,
  and how maintainers can inspect the project.
- Improve navigation and discovery from the landing page through the catalog.
- Build trust with verifiable source links, transparent package provenance, and
  an explicit independent-project statement.
- Keep the site responsive, accessible, fast, and compatible with its current
  static Cloudflare Pages deployment.

## Non-goals

- Claiming official partnership, endorsement, or inclusion before it exists.
- Changing registry, CLI, package submission, or deployment behavior.
- Adding unsupported testimonials, adoption metrics, security guarantees, or
  package-vetting claims.

## Approaches considered

1. **Community constellation (selected):** create an original connected-node
   mark with a considered wordmark, then organize the landing page around the
   community ecosystem, discovery path, verifiable project details, and
   maintainer review links. This best signals a community directory while
   leaving room for a trustworthy, technical tone.
2. **OCM monogram:** build a lettermark and use a documentation-inspired
   editorial layout. This is compact and polished but less immediately
   expressive of a package ecosystem.
3. **Refine the existing spark:** retain the current ring-and-spark concept and
   polish its proportions and colors. This minimizes change but is less likely
   to deliver the requested distinctive identity.

The user delegated the visual-direction choice; the community constellation is
the design call because it connects the logo to OCM's purpose without
suggesting OpenCode ownership.

## Design direction

Use a restrained, high-contrast developer-tool aesthetic with a memorable
constellation-like symbol, custom-feeling wordmark, clear typographic hierarchy,
and purposeful layout rhythm. The mark should remain recognizable as a favicon
and at small sizes, work on both themes, and avoid relying on gradients or tiny
details for its identity. Give the landing page a clear hierarchy: identity and
value proposition, primary browse action, concise proof and transparency,
discovery preview, and a maintainer-facing project/review path. Preserve direct
access to the full catalog and its search/filter behavior.

## Constraints and evidence

- Existing app uses Next.js App Router, Tailwind CSS, and static export to
  Cloudflare Pages (`out`).
- Main UI touchpoints are `LandingHome`, `PackageFinder`, `Header`, `Footer`,
  `MarketplaceHome`, global styles, and public logo/favicon assets.
- Derive any counts or source/provenance statements from repository data and
  implementation; do not invent them.
- Keep existing catalog URLs and interactions unless a change is necessary for
  the redesigned experience.

## Acceptance criteria

- The new mark and wordmark are distinct, readable at small sizes, and work in
  light and dark themes.
- A first-time visitor can tell what OCM is, that it is independent, and how
  to browse or install packages without scrolling through a wall of copy.
- Maintainers can quickly find the source repository, registry/CLI details, and
  a clear invitation to review or provide feedback.
- Marketplace search, filters, URL-synced state, pagination, and mobile layout
  remain usable.
- No unsupported official-status, quality, safety, or popularity claims appear.
- Production build succeeds and the updated static export contains the new
  landing page and assets.
