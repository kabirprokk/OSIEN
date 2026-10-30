/**
 * KeepTheStyle - Keyboard Shortcuts
 * Manages global keyboard shortcuts
 */

class KeyboardShortcuts {
    constructor() {
        this.store = window.store;
        this.shortcuts = {
            'ctrl+z': 'undo',
            'ctrl+shift+z': 'redo',
            'ctrl+y': 'redo',
            'ctrl+s': 'save',
            'ctrl+o': 'open',
            'ctrl+n': 'new',
            'ctrl+d': 'duplicate',
            'delete': 'delete',
            'backspace': 'delete',
            'escape': 'deselect',
            'ctrl+a': 'selectAll'
        };
        
        this.bindEvents();
    }

    bindEvents() {
        document.addEventListener('keydown', this.handleKeyDown.bind(this));
    }

    handleKeyDown(e) {
        // Build shortcut key string
        const keys = [];
        if (e.ctrlKey || e.metaKey) keys.push('ctrl');
        if (e.shiftKey) keys.push('shift');
        if (e.altKey) keys.push('alt');
        keys.push(e.key.toLowerCase());
        
        const shortcut = keys.join('+');
        const action = this.shortcuts[shortcut];
        
        if (action) {
            e.preventDefault();
            this.executeAction(action);
        }
    }

    executeAction(action) {
        switch(action) {
            case 'undo':
                this.store.undo();
                break;
            case 'redo':
                this.store.redo();
                break;
            case 'save':
                // Trigger save
                document.querySelector('[title="Save"]')?.click();
                break;
            case 'open':
                document.querySelector('[title="Open"]')?.click();
                break;
            case 'new':
                document.querySelector('[title="New Project"]')?.click();
                break;
            case 'duplicate':
                const state = this.store.getState();
                if (state.selectedElements.length === 1) {
                    this.store.duplicateElement(state.selectedElements[0]);
                }
                break;
            case 'delete':
                const stateDel = this.store.getState();
                stateDel.selectedElements.forEach(id => {
                    this.store.deleteElement(id);
                });
                break;
            case 'deselect':
                this.store.clearSelection();
                break;
            case 'selectAll':
                const stateAll = this.store.getState();
                stateAll.elements.forEach(el => {
                    this.store.selectElement(el.id);
                });
                break;
        }
    }
}
