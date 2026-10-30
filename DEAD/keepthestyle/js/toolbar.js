/**
 * KeepTheStyle - Toolbar Management
 * Handles top toolbar interactions
 */

class ToolbarManager {
    constructor(container) {
        this.container = container;
        this.store = window.store;
        this.isDarkMode = false;
        
        this.bindEvents();
        this.updateUI();
    }

    bindEvents() {
        // New Project
        this.container.querySelector('[title="New Project"]').addEventListener('click', () => {
            this.newProject();
        });
        
        // Open
        this.container.querySelector('[title="Open"]').addEventListener('click', () => {
            this.openProject();
        });
        
        // Save
        this.container.querySelector('[title="Save"]').addEventListener('click', () => {
            this.saveProject();
        });
        
        // Undo
        this.container.querySelector('[title="Undo"]').addEventListener('click', () => {
            this.undo();
        });
        
        // Redo
        this.container.querySelector('[title="Redo"]').addEventListener('click', () => {
            this.redo();
        });
        
        // Import HTML
        this.container.querySelector('[title="Import HTML"]').addEventListener('click', () => {
            this.importHTML();
        });
        
        // Export
        this.container.querySelector('[title="Export"]').addEventListener('click', () => {
            this.exportProject();
        });
        
        // Preview
        this.container.querySelector('[title="Preview"]').addEventListener('click', () => {
            this.preview();
        });
        
        // Dark Mode toggle
        this.container.querySelector('[title="Dark Mode"]').addEventListener('click', () => {
            this.toggleDarkMode();
        });
        
        // Settings
        this.container.querySelector('[title="Settings"]').addEventListener('click', () => {
            this.openSettings();
        });
        
        // Page size change
        this.container.querySelector('.page-size-select').addEventListener('change', (e) => {
            const [width, height] = e.target.value.split('x').map(Number);
            this.store.setState({ pageSize: { width, height } });
        });
    }

    newProject() {
        if (confirm('Start a new project? Unsaved changes will be lost.')) {
            this.store.setState({
                elements: [],
                projectName: 'Untitled Project',
                selectedElements: []
            });
            this.store.saveHistory();
            this.updateUI();
            this.showNotification('New project created');
        }
    }

    openProject() {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.json,.ktstyle';
        input.onchange = (e) => {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (event) => {
                    try {
                        const data = JSON.parse(event.target.result);
                        if (this.store.importProject(data)) {
                            this.updateUI();
                            this.showNotification('Project loaded successfully');
                        }
                    } catch (err) {
                        this.showNotification('Failed to load project');
                    }
                };
                reader.readAsText(file);
            }
        };
        input.click();
    }

    saveProject() {
        const data = this.store.exportProject();
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.store.getState().projectName}.ktstyle`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.showNotification('Project saved');
    }

    undo() {
        if (this.store.undo()) {
            this.showNotification('Undo');
        }
    }

    redo() {
        if (this.store.redo()) {
            this.showNotification('Redo');
        }
    }

    importHTML() {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.html';
        input.onchange = (e) => {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (event) => {
                    // Parse HTML and convert to elements
                    const html = event.target.result;
                    this.parseHTML(html);
                };
                reader.readAsText(file);
            }
        };
        input.click();
    }

    parseHTML(html) {
        // Simple HTML parsing - can be enhanced
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const body = doc.body;
        
        // Extract elements from body
        const elements = this.extractElements(body);
        if (elements.length > 0) {
            this.store.setState({ elements });
            this.store.saveHistory();
            this.showNotification('HTML imported successfully');
        }
    }

    extractElements(node) {
        const elements = [];
        const children = node.children;
        
        for (let i = 0; i < children.length; i++) {
            const child = children[i];
            const el = {
                id: this.store.generateId(),
                tag: child.tagName.toLowerCase(),
                position: { x: 100 + i * 20, y: 100 + i * 20 },
                size: { width: 200, height: 150 },
                styles: {},
                content: child.textContent || '',
                attributes: {}
            };
            
            // Extract styles
            const style = child.getAttribute('style');
            if (style) {
                style.split(';').forEach(rule => {
                    const [key, value] = rule.split(':').map(s => s.trim());
                    if (key && value) {
                        el.styles[key] = value;
                    }
                });
            }
            
            // Extract attributes
            Array.from(child.attributes).forEach(attr => {
                if (attr.name !== 'style') {
                    el.attributes[attr.name] = attr.value;
                }
            });
            
            elements.push(el);
            
            // Recursively process children
            if (child.children.length > 0) {
                const childElements = this.extractElements(child);
                if (childElements.length > 0) {
                    // Add child elements (adjust positions)
                    childElements.forEach(ce => {
                        ce.position.x += 50;
                        ce.position.y += 50;
                        elements.push(ce);
                    });
                }
            }
        }
        
        return elements;
    }

    exportProject() {
        // Show export options
        const options = ['Export HTML', 'Export CSS', 'Export ZIP', 'Cancel'];
        // Simple dialog (can be enhanced with a proper modal)
        this.showExportDialog(options);
    }

    showExportDialog(options) {
        // Simple implementation - can be enhanced
        const exportType = prompt('Select export type:\n1. Export HTML\n2. Export CSS\n3. Export ZIP');
        if (exportType === '1') {
            this.exportHTML();
        } else if (exportType === '2') {
            this.exportCSS();
        } else if (exportType === '3') {
            this.exportZIP();
        }
    }

    exportHTML() {
        const state = this.store.getState();
        const html = this.generateFullHTML(state.elements);
        this.downloadFile(html, `${state.projectName}.html`, 'text/html');
        this.showNotification('HTML exported');
    }

    generateFullHTML(elements) {
        let html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${this.store.getState().projectName}</title>
    <style>
        /* Generated CSS */
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Inter', -apple-system, sans-serif;
        }
    </style>
</head>
<body>
`;
        
        elements.forEach(el => {
            html += this.renderElementHTML(el, 1);
        });
        
        html += `</body>
</html>`;
        return html;
    }

    renderElementHTML(element, indent) {
        const spaces = '    '.repeat(indent);
        const tag = element.tag || 'div';
        const content = element.content || '';
        const styles = this.renderInlineStyles(element.styles || {});
        const attrs = this.renderAttributes(element.attributes || {});
        
        return `${spaces}<${tag}${attrs} style="${styles}">${content}</${tag}>\n`;
    }

    renderInlineStyles(styles) {
        let css = '';
        Object.entries(styles).forEach(([key, value]) => {
            if (value && value !== '') {
                css += `${key}: ${value}; `;
            }
        });
        return css;
    }

    renderAttributes(attributes) {
        let html = '';
        Object.entries(attributes).forEach(([key, value]) => {
            if (value && value !== '') {
                html += ` ${key}="${value}"`;
            }
        });
        return html;
    }

    exportCSS() {
        const state = this.store.getState();
        let css = this.generateCSS(state.elements);
        this.downloadFile(css, `${state.projectName}.css`, 'text/css');
        this.showNotification('CSS exported');
    }

    generateCSS(elements) {
        let css = `/* KeepTheStyle Generated CSS */
        
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

`;
        
        elements.forEach(el => {
            const selector = `#${el.id || 'element'}`;
            const styles = el.styles || {};
            
            if (Object.keys(styles).length > 0) {
                css += `${selector} {\n`;
                Object.entries(styles).forEach(([key, value]) => {
                    if (value && value !== '') {
                        css += `    ${key}: ${value};\n`;
                    }
                });
                css += `}\n\n`;
            }
        });
        
        return css;
    }

    exportZIP() {
        // Show a notification that ZIP export is coming soon
        this.showNotification('ZIP export coming soon!');
    }

    downloadFile(content, filename, mimeType) {
        const blob = new Blob([content], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    preview() {
        const state = this.store.getState();
        const html = this.generateFullHTML(state.elements);
        const blob = new Blob([html], { type: 'text/html' });
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
    }

    toggleDarkMode() {
        this.isDarkMode = !this.isDarkMode;
        document.body.classList.toggle('dark-mode', this.isDarkMode);
        this.showNotification(this.isDarkMode ? 'Dark mode enabled' : 'Light mode enabled');
    }

    openSettings() {
        // Simple settings dialog
        alert('Settings:\n\n- Dark Mode: ' + (this.isDarkMode ? 'Enabled' : 'Disabled') + '\n- Auto Save: Enabled\n- Grid: Enabled\n- Snap: Enabled');
    }

    updateUI() {
        const state = this.store.getState();
        const projectNameEl = this.container.querySelector('.project-name');
        if (projectNameEl) {
            projectNameEl.textContent = state.projectName;
        }
        
        // Update page size select
        const select = this.container.querySelector('.page-size-select');
        if (select) {
            const size = `${state.pageSize.width}x${state.pageSize.height}`;
            if ([...select.options].some(opt => opt.value === size)) {
                select.value = size;
            }
        }
    }

    showNotification(message) {
        // Use the same notification system from CodePanel
        const notification = document.createElement('div');
        notification.className = 'notification';
        notification.textContent = message;
        notification.style.cssText = `
            position: fixed;
            bottom: 220px;
            right: 20px;
            background: #4D6BFF;
            color: white;
            padding: 12px 20px;
            border-radius: 4px;
            font-family: 'Inter', sans-serif;
            font-size: 14px;
            z-index: 9999;
            animation: slideIn 0.3s ease;
        `;
        
        document.body.appendChild(notification);
        
        setTimeout(() => {
            notification.style.opacity = '0';
            notification.style.transition = 'opacity 0.3s ease';
            setTimeout(() => {
                document.body.removeChild(notification);
            }, 300);
        }, 3000);
    }
}
