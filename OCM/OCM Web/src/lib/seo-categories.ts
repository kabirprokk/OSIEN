import type { PackageType } from "@/lib/registry";

export type SeoCategory = {
  slug: string;
  type: PackageType;
  label: string;
  title: string;
  description: string;
  heading: string;
  intro: string;
  guidanceTitle: string;
  guidance: string;
  faq: { question: string; answer: string }[];
};

export const SEO_CATEGORIES: SeoCategory[] = [
  {
    slug: "opencode-skills",
    type: "skill",
    label: "OpenCode skills",
    title: "OpenCode Skills Directory — Community Skills",
    description: "Find community-built OpenCode skills for coding, research, design, cloud, media, and more. Browse descriptions, tags, source repositories, and install guidance.",
    heading: "OpenCode skills for real workflows",
    intro: "Browse community-built skills that give OpenCode focused instructions and reusable workflows for development, research, design, cloud tools, and creative work.",
    guidanceTitle: "Choose a skill and inspect its source",
    guidance: "Compare each skill’s description and tags with the task you want help with. Open the package details to review its source repository and installation notes before adding community instructions to your setup.",
    faq: [
      { question: "What is an OpenCode skill?", answer: "A skill is a reusable set of instructions and workflow guidance that helps an AI coding agent handle a particular kind of task." },
      { question: "How do I install a skill from this directory?", answer: "For packages supported by the OCM CLI, install with `ocm install <package-name>`. Review the source repository first; community submissions are not endorsed or security-audited by this directory." },
    ],
  },
  {
    slug: "opencode-agents",
    type: "agent",
    label: "OpenCode agents",
    title: "OpenCode Agents Directory — Community AI Agents",
    description: "Explore community-defined OpenCode agents for focused development, research, testing, data, and operations work. Review each agent’s role and source before installing.",
    heading: "Community agents for OpenCode",
    intro: "Find role-focused agent definitions for specialized tasks. Listings include a description, author, license, tags, and a link to the repository so you can assess fit before installing.",
    guidanceTitle: "Pick agents by responsibility",
    guidance: "Start with the agent’s stated scope and the repository instructions it relies on. Check its source and license, then install only the agents that fit your project’s workflow.",
    faq: [
      { question: "What does an OpenCode agent do?", answer: "An agent definition describes a specialized assistant role, its responsibilities, and how it should approach assigned work." },
      { question: "Can I inspect an agent before installing it?", answer: "Yes. Each listing links to its source repository and shows its description and metadata. Review those instructions before installing community content." },
    ],
  },
  {
    slug: "opencode-commands",
    type: "command",
    label: "OpenCode commands",
    title: "OpenCode Commands Directory — Community Workflows",
    description: "Browse reusable community command prompts and workflows for OpenCode. Compare package descriptions, tags, authors, licenses, and source repositories.",
    heading: "Reusable commands for OpenCode",
    intro: "Explore community command definitions designed to make repeatable tasks easier to invoke. Check each command’s purpose and source before adding it to your workflow.",
    guidanceTitle: "Review what each command will ask the agent to do",
    guidance: "Command definitions can shape an agent’s actions. Read the full source, confirm the requested permissions and side effects, and only install commands you trust.",
    faq: [
      { question: "What are OpenCode commands?", answer: "Commands are reusable prompt or workflow definitions that can be invoked for recurring tasks in an OpenCode setup." },
      { question: "Are community commands reviewed?", answer: "No. OCM is an independent directory. Listings link to their source so you can review them yourself before use." },
    ],
  },
  {
    slug: "opencode-mcp-servers",
    type: "mcp",
    label: "OpenCode MCP servers",
    title: "OpenCode MCP Servers — Community Directory",
    description: "Find MCP servers listed for OpenCode and compatible AI clients. Review server capabilities, setup instructions, and source repositories before connecting tools.",
    heading: "MCP servers for OpenCode",
    intro: "Browse Model Context Protocol servers in the community registry. Review what tools a server exposes, where its source lives, and how its maintainers recommend configuring it.",
    guidanceTitle: "Check permissions and setup before connecting",
    guidance: "MCP servers may expose tools that can read data or perform actions. Inspect the repository, understand its configuration and permissions, and follow the server’s own setup guidance.",
    faq: [
      { question: "What is an MCP server?", answer: "An MCP server exposes tools or context through the Model Context Protocol so a compatible AI client can use them." },
      { question: "Does `ocm install` install MCP servers?", answer: "No. MCP servers require their own setup and client configuration. Open a listing’s details and source repository for the server’s installation instructions." },
    ],
  },
  {
    slug: "opencode-skill-packs",
    type: "skill-pack",
    label: "OpenCode skill packs",
    title: "OpenCode Skill Packs — Community Toolkits",
    description: "Discover OpenCode skill packs that bundle community skills, agents, and commands. Review included files, package details, and source before installing a toolkit.",
    heading: "OpenCode skill packs and bundled toolkits",
    intro: "Skill packs collect related community resources into a broader toolkit. Explore the pack description and source before deciding which included skills, agents, or commands belong in your setup.",
    guidanceTitle: "Know what a pack includes",
    guidance: "A pack may add multiple instruction files to your configuration. Review its contents and install paths in the source repository, then use the OCM CLI if the pack’s install method is supported.",
    faq: [
      { question: "What is included in an OpenCode skill pack?", answer: "Contents vary by pack; a pack can bundle skills, agents, commands, or related setup files. Inspect the repository to see the exact contents." },
      { question: "Can I review a pack before installing it?", answer: "Yes. OCM links to the source repository and package details. Review the files and installation paths before adding the pack." },
    ],
  },
];

export function getSeoCategory(slug: string) {
  return SEO_CATEGORIES.find((category) => category.slug === slug);
}

export function getSeoCategoryForType(type: PackageType) {
  return SEO_CATEGORIES.find((category) => category.type === type);
}

// Compile-time guard: supported package types and SEO category definitions must
// stay in sync. If a type is added/removed in either place, this fails to build.
type _CategoryTypeParity =
  PackageType extends (typeof SEO_CATEGORIES)[number]["type"]
    ? (typeof SEO_CATEGORIES)[number]["type"] extends PackageType
      ? true
      : never
    : never;
const _categoryTypeParity: _CategoryTypeParity = true;
void _categoryTypeParity;
