import React, { createContext, useContext, useState, useEffect } from 'react';
import { ProjectItem, StudioConfig } from '../types';
import { STUDIO_CONFIG as DEFAULT_STUDIO_CONFIG, PROJECTS_DATA as DEFAULT_PROJECTS } from '../config/stellerNebulaConfig';

interface ContentContextType {
  projects: ProjectItem[];
  studioConfig: StudioConfig;
  isUsingCustomData: boolean;
  addProject: (project: Omit<ProjectItem, 'id'> & { id?: string }) => { success: boolean; error?: string };
  updateProject: (id: string, updated: Partial<ProjectItem>) => { success: boolean; error?: string };
  deleteProject: (id: string) => void;
  moveProject: (index: number, direction: 'up' | 'down') => void;
  updateStudioConfig: (updated: Partial<StudioConfig>) => void;
  exportBackup: () => void;
  importBackup: (jsonContent: string) => { success: boolean; error?: string };
  resetToDefault: () => void;
}

const LOCAL_STORAGE_PROJECTS_KEY = 'steller_nebula_local_projects_v2';
const LOCAL_STORAGE_CONFIG_KEY = 'steller_nebula_local_config_v2';

// Helper to sanitize and normalize old strings to have the dash
const normalizeStudioConfig = (config: any): StudioConfig => {
  const merged: StudioConfig = {
    ...DEFAULT_STUDIO_CONFIG,
    ...config,
    developer: {
      ...DEFAULT_STUDIO_CONFIG.developer,
      ...(config?.developer || {})
    },
    howIBuild:
      config?.howIBuild && Array.isArray(config.howIBuild) && config.howIBuild.length > 0
        ? config.howIBuild
        : DEFAULT_STUDIO_CONFIG.howIBuild
  };

  // Ensure the dash is always present in STELLER-NEBULA / Steller-Nebula
  if (merged.studioName) {
    merged.studioName = merged.studioName.replace(/\bSTELLERNEBULA\b/gi, 'STELLER-NEBULA');
  }
  if (merged.heroHeadline) {
    merged.heroHeadline = merged.heroHeadline.replace(/\bSTELLERNEBULA\b/gi, 'STELLER-NEBULA');
  }
  if (merged.heroSupportingText) {
    merged.heroSupportingText = merged.heroSupportingText.replace(/StellerNebula/g, 'Steller-Nebula');
  }
  if (merged.aboutText) {
    merged.aboutText = merged.aboutText.replace(/StellerNebula/g, 'Steller-Nebula');
  }

  return merged;
};

const ContentContext = createContext<ContentContextType | undefined>(undefined);

export const ContentProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [projects, setProjects] = useState<ProjectItem[]>(() => {
    try {
      // Check v2 key first, then fallback to v1 key
      const saved = localStorage.getItem(LOCAL_STORAGE_PROJECTS_KEY) || localStorage.getItem('stellernebula_local_projects');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      }
    } catch (e) {
      console.warn('Failed to load local projects from storage:', e);
    }
    return DEFAULT_PROJECTS;
  });

  const [studioConfig, setStudioConfig] = useState<StudioConfig>(() => {
    try {
      // Check v2 key first, then fallback to v1 key
      const saved = localStorage.getItem(LOCAL_STORAGE_CONFIG_KEY) || localStorage.getItem('stellernebula_local_config');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (parsed && typeof parsed === 'object') {
          return normalizeStudioConfig(parsed);
        }
      }
    } catch (e) {
      console.warn('Failed to load local config from storage:', e);
    }
    return DEFAULT_STUDIO_CONFIG;
  });

  // Track if user has custom local overrides
  const [isUsingCustomData, setIsUsingCustomData] = useState<boolean>(() => {
    return (
      localStorage.getItem(LOCAL_STORAGE_PROJECTS_KEY) !== null ||
      localStorage.getItem(LOCAL_STORAGE_CONFIG_KEY) !== null
    );
  });

  // Persist projects whenever they change
  const saveProjectsToStorage = (updatedProjects: ProjectItem[]) => {
    setProjects(updatedProjects);
    try {
      localStorage.setItem(LOCAL_STORAGE_PROJECTS_KEY, JSON.stringify(updatedProjects));
      setIsUsingCustomData(true);
    } catch (e) {
      console.error('Failed to save projects to localStorage:', e);
    }
  };

  // Persist config whenever it changes
  const saveConfigToStorage = (updatedConfig: StudioConfig) => {
    const normalized = normalizeStudioConfig(updatedConfig);
    setStudioConfig(normalized);
    try {
      localStorage.setItem(LOCAL_STORAGE_CONFIG_KEY, JSON.stringify(normalized));
      setIsUsingCustomData(true);
    } catch (e) {
      console.error('Failed to save config to localStorage:', e);
    }
  };

  const addProject = (projectData: Omit<ProjectItem, 'id'> & { id?: string }) => {
    if (!projectData.name || !projectData.name.trim()) {
      return { success: false, error: 'Project name is required.' };
    }
    if (!projectData.description || !projectData.description.trim()) {
      return { success: false, error: 'Project description is required.' };
    }

    const generatedId =
      projectData.id?.trim() ||
      projectData.name
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/(^-|-$)/g, '') ||
      `project-${Date.now()}`;

    // Verify uniqueness
    if (projects.some((p) => p.id === generatedId)) {
      return { success: false, error: `A project with identifier "${generatedId}" already exists.` };
    }

    const newProject: ProjectItem = {
      id: generatedId,
      name: projectData.name.trim(),
      category: projectData.category?.trim() || 'General',
      description: projectData.description.trim(),
      tagline: projectData.tagline?.trim() || '',
      url: projectData.url?.trim() || '',
      status: projectData.status || 'Active',
      year: projectData.year || new Date().getFullYear().toString(),
      features: projectData.features || [],
      themeColor: projectData.themeColor || '#00F0FF'
    };

    const nextProjects = [...projects, newProject];
    saveProjectsToStorage(nextProjects);
    return { success: true };
  };

  const updateProject = (id: string, updated: Partial<ProjectItem>) => {
    const index = projects.findIndex((p) => p.id === id);
    if (index === -1) {
      return { success: false, error: 'Project not found.' };
    }

    if (updated.name !== undefined && !updated.name.trim()) {
      return { success: false, error: 'Project name cannot be empty.' };
    }
    if (updated.description !== undefined && !updated.description.trim()) {
      return { success: false, error: 'Project description cannot be empty.' };
    }

    const updatedProjects = [...projects];
    updatedProjects[index] = {
      ...updatedProjects[index],
      ...updated
    };

    saveProjectsToStorage(updatedProjects);
    return { success: true };
  };

  const deleteProject = (id: string) => {
    const filtered = projects.filter((p) => p.id !== id);
    saveProjectsToStorage(filtered);
  };

  const moveProject = (index: number, direction: 'up' | 'down') => {
    if (direction === 'up' && index === 0) return;
    if (direction === 'down' && index === projects.length - 1) return;

    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    const reordered = [...projects];
    const [moved] = reordered.splice(index, 1);
    reordered.splice(targetIndex, 0, moved);

    saveProjectsToStorage(reordered);
  };

  const updateStudioConfig = (updated: Partial<StudioConfig>) => {
    const nextConfig = {
      ...studioConfig,
      ...updated
    };
    saveConfigToStorage(nextConfig);
  };

  const exportBackup = () => {
    const backupData = {
      version: '1.0',
      timestamp: new Date().toISOString(),
      studioConfig,
      projects
    };

    const blob = new Blob([JSON.stringify(backupData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `steller-nebula-backup-${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const importBackup = (jsonContent: string) => {
    try {
      const data = JSON.parse(jsonContent);

      if (!data || typeof data !== 'object') {
        return { success: false, error: 'Invalid backup file: Not a valid JSON object.' };
      }

      if (!Array.isArray(data.projects)) {
        return { success: false, error: 'Invalid backup file: Missing or invalid "projects" list.' };
      }

      // Basic project validation
      for (const p of data.projects) {
        if (!p.id || !p.name || !p.description) {
          return {
            success: false,
            error: `Invalid backup file: Project "${p.name || 'unnamed'}" is missing required fields.`
          };
        }
      }

      const importedProjects = data.projects as ProjectItem[];
      const importedConfig = data.studioConfig ? { ...DEFAULT_STUDIO_CONFIG, ...data.studioConfig } : studioConfig;

      saveProjectsToStorage(importedProjects);
      saveConfigToStorage(importedConfig);

      return { success: true };
    } catch (err: any) {
      return { success: false, error: `Failed to parse backup JSON: ${err.message || 'Syntax error'}` };
    }
  };

  const resetToDefault = () => {
    localStorage.removeItem(LOCAL_STORAGE_PROJECTS_KEY);
    localStorage.removeItem(LOCAL_STORAGE_CONFIG_KEY);
    localStorage.removeItem('stellernebula_local_projects');
    localStorage.removeItem('stellernebula_local_config');
    setProjects(DEFAULT_PROJECTS);
    setStudioConfig(DEFAULT_STUDIO_CONFIG);
    setIsUsingCustomData(false);
  };

  return (
    <ContentContext.Provider
      value={{
        projects,
        studioConfig,
        isUsingCustomData,
        addProject,
        updateProject,
        deleteProject,
        moveProject,
        updateStudioConfig,
        exportBackup,
        importBackup,
        resetToDefault
      }}
    >
      {children}
    </ContentContext.Provider>
  );
};

export const useContent = () => {
  const context = useContext(ContentContext);
  if (!context) {
    throw new Error('useContent must be used within a ContentProvider');
  }
  return context;
};
