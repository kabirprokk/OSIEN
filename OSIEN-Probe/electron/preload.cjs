'use strict';
const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('deepcheck', {
  pickFolder: () => ipcRenderer.invoke('pick-folder'),
  listProjects: () => ipcRenderer.invoke('list-projects'),
  addProject: (payload) => ipcRenderer.invoke('add-project', payload),
  cloneRepo: (payload) => ipcRenderer.invoke('clone-repo', payload),
  removeProject: (id) => ipcRenderer.invoke('remove-project', id),
  openProject: (id) => ipcRenderer.invoke('open-project', id),
  openDataFolder: () => ipcRenderer.invoke('open-data-folder'),
  scanProject: (id) => ipcRenderer.invoke('scan-project', id),
  verifyFinding: (payload) => ipcRenderer.invoke('verify-finding', payload),
  getFindingContext: (payload) => ipcRenderer.invoke('get-finding-context', payload),
  checkAiStatus: () => ipcRenderer.invoke('check-ai-status'),
  exportReport: (payload) => ipcRenderer.invoke('export-report', payload),
  runProjectTests: (id) => ipcRenderer.invoke('run-project-tests', id),
  runProjectTestFile: (payload) => ipcRenderer.invoke('run-project-test-file', payload),
  cancelAiReview: (projectId) => ipcRenderer.invoke('cancel-ai-review', projectId),
  aiReviewStatus: () => ipcRenderer.invoke('ai-review-status'),
  onAiReviewProgress: (callback) => {
    const listener = (_event, payload) => callback(payload);
    ipcRenderer.on('ai-review-progress', listener);
    return () => ipcRenderer.removeListener('ai-review-progress', listener);
  },
});
