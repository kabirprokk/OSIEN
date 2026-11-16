import { StudioConfig, ProjectItem } from '../types';

export const STUDIO_CONFIG: StudioConfig = {
  studioName: 'STELLER-NEBULA',
  tagline: 'A place for ideas, projects, and things I want to build.',
  heroHeadline: 'STELLER-NEBULA',
  heroSubtitle: 'A place for ideas, projects, and things I want to build.',
  heroSupportingText:
    'Steller-Nebula is where I build, explore, and share the things I care about — from software and websites to creative tools and experiments.',
  aboutIntro: 'A place for the things I build, explore, and bring to life.',
  aboutText:
    'Steller-Nebula is my independent home for the projects I create. It brings together my websites, software, creative tools, and experiments in one place. It is not a huge company or a collection of products made just to grow. It is simply a space where I can build things I find interesting and let them evolve naturally.',
  whyItExists:
    'I wanted a place where all my projects could live together without making each one feel disconnected. Steller-Nebula became that space — a home for the ideas I turn into real things.',
  whatsNext:
    'There is always something new to explore. Steller-Nebula will continue to grow as I build, experiment, and find new ideas worth bringing to life.',
  howIBuild: [
    {
      title: 'Turn ideas into real things.',
      description:
        'I like taking an idea from a rough thought and turning it into something people can actually use, explore, or experience.'
    },
    {
      title: 'Stay curious and keep experimenting.',
      description:
        'I enjoy trying new technologies, creative approaches, and different ways of building things without being afraid to start over.'
    },
    {
      title: 'Care about the little things.',
      description:
        'Good projects are not only about making something work. I care about the experience, the details, and making everything feel intentional.'
    }
  ],
  developer: {
    name: 'Kabir Sayed',
    bio:
      'I’m a developer who enjoys turning ideas into things people can actually use. Steller-Nebula is where I bring those ideas together — from websites and software to creative experiments and things I’m simply curious enough to build.',
    avatarUrl: '',
    interests: [
      'Software Development',
      'Creative Technology',
      'Web Applications',
      'Experimental Tools'
    ]
  },
  contactEmail: 'kabirsayed.k@gmail.com',
  socials: [
    {
      platform: 'GitHub',
      url: 'https://github.com/kabirprokk',
      handle: 'GitHub',
      icon: 'Github'
    },
    {
      platform: 'Instagram',
      url: 'https://www.instagram.com/kabir.found.neogen/',
      handle: 'Instagram',
      icon: 'Instagram'
    }
  ],
  year: '2026'
};

/**
 * Real projects source of truth.
 * ONLY add projects here when they actually exist.
 * Never add fictional or placeholder projects.
 */
export const PROJECTS_DATA: ProjectItem[] = [
  {
    id: 'storyteller',
    name: 'Storyteller',
    category: 'Creative Publishing',
    description:
      'A cinematic space for sharing stories, ideas, and voices that deserve to be heard.',
    tagline: 'Stories worth staying for.',
    url: 'https://storyteller-page-v1.vercel.app',
    status: 'ACTIVE',
    year: '2026',
    features: [
      'Cinematic reading experience',
      'Distraction-free typography',
      'Atmospheric visual presentation'
    ],
    themeColor: '#00F0FF'
  },
    {
    id: 'keepthestyle',
    name: 'KeepTheStyle',
    category: 'No-Code Website Builder',
    description:
      'A visual website builder for creating, customizing, and experimenting with websites without starting from scratch in code.',
    tagline: 'Build it. Style it. Make it yours.',
    url: 'https://keepthestyle-v1.vercel.app',
    status: 'ACTIVE',
    year: '2026',
    features: [
      'Visual website building',
      'Drag-and-drop editing',
      'Live styling and customization',
      'HTML, CSS, and JavaScript controls'
    ],
    themeColor: '#00F0FF'
  },
    {
  id: 'writersvelocity',
  name: 'WritersVelocity',
  category: 'Writing & Productivity',
  description:
    'A focused writing tool designed to help writers track their writing pace, measure progress, and turn ideas into consistent writing.',
  tagline: 'Write with momentum.',
  url: 'https://writersvelocity.netlify.app',
  status: 'ACTIVE',
  year: '2025',
  features: [
    'Writing velocity tracking',
    'Real-time writing statistics',
    'Focused writing experience',
    'Progress and productivity insights'
  ],
  themeColor: '#00F0FF'
},
  {
    id: 'py-editor',
    name: 'Py_Editor',
    category: 'Python Development',
    description:
      'A lightweight Python editor built for writing, editing, and experimenting with Python directly in the browser.',
    tagline: 'Write Python. Explore. Build.',
    url: 'https://pyeditorv2.vercel.app',
    status: 'ACTIVE',
    year: '2025',
    features: [
      'Browser-based Python editor',
      'Multiple editor themes',
      'Simple file editing',
      'Focused coding experience'
    ],
    themeColor: '#00F0FF'
  },
  {
  id: 'luxe-temporal',
  name: 'LUXE · temporal',
  category: 'Time & Utility',
  description:
    'A minimalist digital clock designed to make time feel calm, clear, and beautifully simple.',
  tagline: 'Time, beautifully simple.',
  url: 'https://luxe-temporal-clock.netlify.app',
  status: 'ACTIVE',
  year: '2025',
  features: [
    'Live digital clock',
    'Minimalist interface',
    'Real-time date display',
    'Focused time experience'
  ],
  themeColor: '#00F0FF'
}
];
