/**
 * KeepTheStyle - Canvas Manager
 * Handles the canvas rendering and element interaction
 */

class CanvasManager {
    constructor(container) {
        this.container = container;
        this.canvasPage = container.querySelector('.canvas-page');
        this.canvasWrapper = container.querySelector('.canvas-wrapper');
        this.store = window.store;
        
        this.isDragging = false;
        this.dragTarget = null;
        this.dragOffset = { x: 0, y: 0 };
        this.zoomLevel = 1;
        
        this.init();
    }

    init() {
        this.bindEvents();
        this.subscribeToStore();
        this.renderCanvas();
    }

    subscribeToStore() {
        this.store.subscribe((state) => {
            // Real-time render when state changes
            this.renderCanvas();
        });
    }

    bindEvents() {
        // Zoom controls
        const zoomBtns = this.container.querySelectorAll('.zoom-btn');
        zoomBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const action = btn.dataset.action;
                this.handleZoom(action);
            });
        });

        // Canvas controls
        const canvasBtns = this.container.querySelectorAll('.canvas-btn');
        canvasBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const action = btn.dataset.action;
                this.handleCanvasControl(action);
            });
        });

        // Page size
        const pageSelect = this.container.querySelector('.page-size-select');
        pageSelect.addEventListener('change', (e) => {
            const [width, height] = e.target.value.split('x').map(Number);
            this.store.setState({ pageSize: { width, height } });
        });

        // Drag and drop from sidebar
        this.canvasPage.addEventListener('dragover', (e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'copy';
        });

        this.canvasPage.addEventListener('drop', (e) => {
            e.preventDefault();
            const data = e.dataTransfer.getData('text/plain');
            if (data) {
                try {
                    const elementData = JSON.parse(data);
                    const wrapperRect = this.canvasWrapper.getBoundingClientRect();
                    const x = (e.clientX - wrapperRect.left) / this.zoomLevel;
                    const y = (e.clientY - wrapperRect.top) / this.zoomLevel;
                    
                    const defaults = elementData.defaults || {};
                    const newElement = {
                        tag: elementData.tag,
                        position: { x: Math.max(0, x - 100), y: Math.max(0, y - 75) },
                        size: { width: 200, height: 150 },
                        styles: defaults.styles || {},
                        content: defaults.content || '',
                        attributes: defaults.attributes || {}
                    };
                    this.store.addElement(newElement);
                } catch (err) {
                    console.warn('Failed to parse dragged element:', err);
                }
            }
        });

        // Clear selection on canvas click
        this.canvasPage.addEventListener('mousedown', (e) => {
            if (e.target === this.canvasPage) {
                this.store.clearSelection();
            }
        });
    }

    handleZoom(action) {
        switch(action) {
            case 'zoom-in':
                this.zoomLevel = Math.min(this.zoomLevel + 0.1, 3);
                break;
            case 'zoom-out':
                this.zoomLevel = Math.max(this.zoomLevel - 0.1, 0.1);
                break;
            case 'fit-screen':
                const rect = this.canvasWrapper.getBoundingClientRect();
                const pageRect = this.canvasPage.getBoundingClientRect();
                const scaleX = (rect.width - 40) / pageRect.width;
                const scaleY = (rect.height - 40) / pageRect.height;
                this.zoomLevel = Math.min(scaleX, scaleY, 1);
                break;
        }
        this.updateZoomLevel();
        this.renderCanvas();
    }

    handleCanvasControl(action) {
        const state = this.store.getState();
        switch(action) {
            case 'grid':
                this.store.setState({ gridVisible: !state.gridVisible });
                break;
            case 'guidelines':
                this.store.setState({ guidelinesVisible: !state.guidelinesVisible });
                break;
            case 'rulers':
                this.store.setState({ rulersVisible: !state.rulersVisible });
                break;
        }
    }

    updateZoomLevel() {
        const zoomText = this.container.querySelector('.zoom-level');
        if (zoomText) {
            zoomText.textContent = Math.round(this.zoomLevel * 100) + '%';
        }
    }

    renderCanvas() {
        const state = this.store.getState();
        const elements = state.elements;
        const selectedIds = state.selectedElements;
        
        this.canvasPage.innerHTML = '';
        
        // Update page size
        this.canvasPage.style.width = state.pageSize.width + 'px';
        this.canvasPage.style.height = state.pageSize.height + 'px';
        this.canvasPage.style.backgroundColor = '#FFFFFF';
        this.canvasPage.style.position = 'relative';
        
        elements.forEach(element => {
            const el = this.createElementNode(element, selectedIds);
            this.canvasPage.appendChild(el);
        });
        
        this.updateZoomLevel();
        this.canvasPage.style.transform = `scale(${this.zoomLevel})`;
        this.canvasPage.style.transformOrigin = 'top left';
    }

    createElementNode(element, selectedIds) {
        const el = document.createElement(element.tag || 'div');
        el.dataset.id = element.id;
        el.dataset.tag = element.tag || 'div';
        
        // Position
        el.style.position = 'absolute';
        if (element.position) {
            el.style.left = element.position.x + 'px';
            el.style.top = element.position.y + 'px';
        }
        
        // Size
        if (element.size) {
            el.style.width = element.size.width + 'px';
            el.style.height = element.size.height + 'px';
        }
        
        // Styles
        if (element.styles) {
            Object.entries(element.styles).forEach(([prop, value]) => {
                try {
                    el.style[prop] = value;
                } catch (e) {
                    console.warn(`Failed to set style ${prop}:`, e);
                }
            });
        }
        
        // Content
        if (element.content && (element.tag === 'div' || element.tag === 'span' || element.tag === 'p' || element.tag === 'button')) {
            el.textContent = element.content;
        }
        
        // Attributes
        if (element.attributes) {
            Object.entries(element.attributes).forEach(([attr, value]) => {
                try {
                    el.setAttribute(attr, value);
                } catch (e) {
                    console.warn(`Failed to set attribute ${attr}:`, e);
                }
            });
        }
        
        // Selection
        if (selectedIds.includes(element.id)) {
            el.classList.add('selected');
            el.style.outline = '2px solid #4D6BFF';
        }
        
        el.style.cursor = 'move';
        el.style.userSelect = 'none';
        
        // Click to select
        el.addEventListener('mousedown', (e) => {
            e.stopPropagation();
            
            if (e.shiftKey) {
                const state = this.store.getState();
                if (state.selectedElements.includes(element.id)) {
                    this.store.deselectElement(element.id);
                } else {
                    this.store.selectElement(element.id);
                }
            } else {
                this.store.clearSelection();
                this.store.selectElement(element.id);
            }
            
            // Start drag
            this.isDragging = true;
            this.dragTarget = el;
            const rect = el.getBoundingClientRect();
            const wrapperRect = this.canvasWrapper.getBoundingClientRect();
            this.dragOffset = {
                x: e.clientX - rect.left,
                y: e.clientY - rect.top
            };
        });
        
        return el;
    }
}

// Mouse events for dragging
document.addEventListener('mousemove', (e) => {
    if (!window.canvasManager || !window.canvasManager.isDragging || !window.canvasManager.dragTarget) return;
    
    const manager = window.canvasManager;
    const id = manager.dragTarget.dataset.id;
    const state = window.store.getState();
    const element = state.elements.find(el => el.id === id);
    
    if (!element) return;
    
    const wrapperRect = manager.canvasWrapper.getBoundingClientRect();
    const x = (e.clientX - wrapperRect.left - manager.dragOffset.x) / manager.zoomLevel;
    const y = (e.clientY - wrapperRect.top - manager.dragOffset.y) / manager.zoomLevel;
    
    window.store.updateElement(id, {
        position: { x: Math.max(0, x), y: Math.max(0, y) }
    });
});

document.addEventListener('mouseup', () => {
    if (window.canvasManager) {
        window.canvasManager.isDragging = false;
        window.canvasManager.dragTarget = null;
    }
});
