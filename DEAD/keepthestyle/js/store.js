/**
 * KeepTheStyle - Application State Management
 * Central store for all application data with real-time updates
 */

class Store {
    constructor() {
        this.state = {
            elements: [],
            selectedElements: [],
            clipboard: null,
            history: [],
            historyIndex: -1,
            maxHistory: 50,
            zoom: 1,
            gridVisible: true,
            guidelinesVisible: true,
            rulersVisible: true,
            snapEnabled: true,
            pageSize: { width: 1920, height: 1080 },
            darkMode: false,
            activeTab: 'html',
            projectName: 'Untitled Project',
            isDragging: false,
            dragOffset: { x: 0, y: 0 },
            selectedTool: 'select',
            lastUpdate: Date.now()
        };

        this.listeners = [];
        this.init();
    }

    init() {
        // Load from localStorage if available
        this.loadFromStorage();
        
        // Auto-save every 5 seconds
        this.autoSaveInterval = setInterval(() => this.saveToStorage(), 5000);
    }

    // Subscribe to state changes
    subscribe(listener) {
        this.listeners.push(listener);
        // Call listener immediately with current state
        listener(this.state);
        return () => {
            this.listeners = this.listeners.filter(l => l !== listener);
        };
    }

    // Notify all listeners of state change in real-time
    notify() {
        this.listeners.forEach(listener => {
            try {
                listener(this.state);
            } catch (e) {
                console.error('Error in store listener:', e);
            }
        });
    }

    // Get current state
    getState() {
        return this.state;
    }

    // Update state with real-time notification
    setState(newState) {
        const oldState = { ...this.state };
        this.state = { ...this.state, ...newState };
        this.state.lastUpdate = Date.now();
        
        // Save to history if significant change
        if (this.shouldSaveHistory(oldState, this.state)) {
            this.saveHistory();
        }
        
        this.notify();
        this.saveToStorage();
    }

    // Check if state change should be saved to history
    shouldSaveHistory(oldState, newState) {
        // Compare elements array for changes
        if (JSON.stringify(oldState.elements) !== JSON.stringify(newState.elements)) {
            return true;
        }
        return false;
    }

    // History management
    saveHistory() {
        // Remove any future history if we're not at the end
        if (this.state.historyIndex < this.state.history.length - 1) {
            this.state.history = this.state.history.slice(0, this.state.historyIndex + 1);
        }
        
        // Add current state to history
        const snapshot = JSON.stringify(this.state.elements);
        this.state.history.push(snapshot);
        this.state.historyIndex = this.state.history.length - 1;
        
        // Limit history size
        if (this.state.history.length > this.state.maxHistory) {
            this.state.history.shift();
            this.state.historyIndex--;
        }
    }

    undo() {
        if (this.state.historyIndex > 0) {
            this.state.historyIndex--;
            const snapshot = this.state.history[this.state.historyIndex];
            this.state.elements = JSON.parse(snapshot);
            this.state.lastUpdate = Date.now();
            this.notify();
            this.saveToStorage();
            return true;
        }
        return false;
    }

    redo() {
        if (this.state.historyIndex < this.state.history.length - 1) {
            this.state.historyIndex++;
            const snapshot = this.state.history[this.state.historyIndex];
            this.state.elements = JSON.parse(snapshot);
            this.state.lastUpdate = Date.now();
            this.notify();
            this.saveToStorage();
            return true;
        }
        return false;
    }

    // Element operations
    addElement(elementData) {
        const element = {
            ...elementData,
            id: this.generateId(),
            position: elementData.position || { x: 100, y: 100 },
            size: elementData.size || { width: 200, height: 150 },
            styles: elementData.styles || {},
            content: elementData.content || '',
            attributes: elementData.attributes || {},
            children: elementData.children || []
        };
        this.state.elements.push(element);
        this.state.selectedElements = [element.id];
        this.saveHistory();
        this.notify();
        this.saveToStorage();
        return element;
    }

    deleteElement(id) {
        this.state.elements = this.state.elements.filter(el => el.id !== id);
        this.state.selectedElements = this.state.selectedElements.filter(sid => sid !== id);
        this.saveHistory();
        this.notify();
        this.saveToStorage();
    }

    updateElement(id, updates) {
        const element = this.state.elements.find(el => el.id === id);
        if (element) {
            Object.assign(element, updates);
            this.notify();
            this.saveToStorage();
        }
    }

    selectElement(id) {
        if (!this.state.selectedElements.includes(id)) {
            this.state.selectedElements.push(id);
            this.notify();
        }
    }

    deselectElement(id) {
        this.state.selectedElements = this.state.selectedElements.filter(sid => sid !== id);
        this.notify();
    }

    clearSelection() {
        this.state.selectedElements = [];
        this.notify();
    }

    duplicateElement(id) {
        const element = this.state.elements.find(el => el.id === id);
        if (element) {
            const newElement = deepClone({
                ...element,
                id: this.generateId(),
                position: {
                    x: element.position.x + 20,
                    y: element.position.y + 20
                }
            });
            this.state.elements.push(newElement);
            this.state.selectedElements = [newElement.id];
            this.saveHistory();
            this.notify();
            this.saveToStorage();
        }
    }

    generateId() {
        return `el_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }

    // Local Storage
    saveToStorage() {
        try {
            const data = {
                elements: this.state.elements,
                projectName: this.state.projectName,
                pageSize: this.state.pageSize,
                lastUpdate: this.state.lastUpdate
            };
            localStorage.setItem('keepthestyle_project', JSON.stringify(data));
        } catch (e) {
            console.warn('Failed to save to localStorage:', e);
        }
    }

    loadFromStorage() {
        try {
            const data = localStorage.getItem('keepthestyle_project');
            if (data) {
                const parsed = JSON.parse(data);
                this.state.elements = parsed.elements || [];
                this.state.projectName = parsed.projectName || 'Untitled Project';
                this.state.pageSize = parsed.pageSize || { width: 1920, height: 1080 };
                this.saveHistory();
            }
        } catch (e) {
            console.warn('Failed to load from localStorage:', e);
        }
    }

    // Export/Import
    exportProject() {
        return {
            elements: this.state.elements,
            projectName: this.state.projectName,
            pageSize: this.state.pageSize,
            version: '1.0.0'
        };
    }

    importProject(data) {
        try {
            this.state.elements = data.elements || [];
            this.state.projectName = data.projectName || 'Imported Project';
            this.state.pageSize = data.pageSize || { width: 1920, height: 1080 };
            this.saveHistory();
            this.notify();
            this.saveToStorage();
            return true;
        } catch (e) {
            console.error('Failed to import project:', e);
            return false;
        }
    }

    destroy() {
        if (this.autoSaveInterval) {
            clearInterval(this.autoSaveInterval);
        }
    }
}

// Create singleton instance
const store = new Store();
