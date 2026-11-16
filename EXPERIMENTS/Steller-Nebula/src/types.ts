export interface ProjectItem {
  id: string;
  name: string;
  category: string;
  description: string;
  tagline?: string;
  url?: string;
  githubUrl?: string;
  status?: string;
  year?: string;
  features?: string[];
  themeColor?: string;
}

export interface SocialLink {
  platform: string;
  url: string;
  handle?: string;
  icon?: string;
}

export interface DeveloperInfo {
  name: string;
  bio: string;
  avatarUrl?: string;
  interests?: string[];
}

export interface StudioConfig {
  studioName: string;
  tagline: string;
  heroHeadline: string;
  heroSubtitle: string;
  heroSupportingText: string;
  aboutIntro: string;
  aboutText: string;
  whyItExists: string;
  whatsNext: string;
  howIBuild: {
    title: string;
    description: string;
  }[];
  aboutHighlights?: {
    title: string;
    description: string;
  }[];
  developer: DeveloperInfo;
  contactEmail: string;
  socials: SocialLink[];
  year: string;
}
