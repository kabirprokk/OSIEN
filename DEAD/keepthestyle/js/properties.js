/**
 * KeepTheStyle - Properties Panel
 * Manages the right sidebar with element properties
 */

class PropertiesManager {
    constructor(container) {
        this.container = container;
        this.content = container.querySelector('.properties-content');
        this.store = window.store;
        
        this.propertyGroups = this.getPropertyGroups();
        this.selectedElement = null;
        
        // Subscribe to store changes
        this.store.subscribe(() => this.update());
        
        // Initialize
        this.update();
    }

    getPropertyGroups() {
        return {
            'Layout': {
                expanded: true,
                properties: [
                    { key: 'width', label: 'Width', type: 'number', unit: 'px', default: 'auto' },
                    { key: 'height', label: 'Height', type: 'number', unit: 'px', default: 'auto' },
                    { key: 'minWidth', label: 'Min Width', type: 'number', unit: 'px', default: 'auto' },
                    { key: 'maxWidth', label: 'Max Width', type: 'number', unit: 'px', default: 'auto' },
                    { key: 'padding', label: 'Padding', type: 'number', unit: 'px', default: '0' },
                    { key: 'margin', label: 'Margin', type: 'number', unit: 'px', default: '0' },
                    { key: 'gap', label: 'Gap', type: 'number', unit: 'px', default: '0' },
                    { key: 'display', label: 'Display', type: 'select', options: ['block', 'inline', 'flex', 'grid', 'none'], default: 'block' },
                    { key: 'position', label: 'Position', type: 'select', options: ['static', 'relative', 'absolute', 'fixed', 'sticky'], default: 'static' },
                    { key: 'overflow', label: 'Overflow', type: 'select', options: ['visible', 'hidden', 'scroll', 'auto'], default: 'visible' }
                ]
            },
            'Typography': {
                expanded: true,
                properties: [
                    { key: 'fontFamily', label: 'Font Family', type: 'text', default: 'Inter' },
                    { key: 'fontSize', label: 'Font Size', type: 'number', unit: 'px', default: '16' },
                    { key: 'fontWeight', label: 'Font Weight', type: 'number', min: 100, max: 900, step: 100, default: '400' },
                    { key: 'lineHeight', label: 'Line Height', type: 'number', min: 1, max: 3, step: 0.1, default: '1.5' },
                    { key: 'letterSpacing', label: 'Letter Spacing', type: 'number', unit: 'px', default: '0' },
                    { key: 'textAlign', label: 'Text Align', type: 'select', options: ['left', 'center', 'right', 'justify'], default: 'left' },
                    { key: 'textDecoration', label: 'Decoration', type: 'select', options: ['none', 'underline', 'line-through', 'overline'], default: 'none' },
                    { key: 'textTransform', label: 'Transform', type: 'select', options: ['none', 'uppercase', 'lowercase', 'capitalize'], default: 'none' },
                    { key: 'color', label: 'Color', type: 'color', default: '#000000' }
                ]
            },
            'Background': {
                expanded: true,
                properties: [
                    { key: 'backgroundColor', label: 'Background Color', type: 'color', default: '#FFFFFF' },
                    { key: 'backgroundImage', label: 'Image', type: 'text', default: 'none' },
                    { key: 'backgroundRepeat', label: 'Repeat', type: 'select', options: ['repeat', 'no-repeat', 'repeat-x', 'repeat-y'], default: 'repeat' },
                    { key: 'backgroundPosition', label: 'Position', type: 'text', default: '0% 0%' },
                    { key: 'backgroundSize', label: 'Size', type: 'text', default: 'auto' },
                    { key: 'opacity', label: 'Opacity', type: 'number', min: 0, max: 1, step: 0.01, default: '1' }
                ]
            },
            'Border': {
                expanded: false,
                properties: [
                    { key: 'borderRadius', label: 'Radius', type: 'number', unit: 'px', default: '0' },
                    { key: 'borderWidth', label: 'Width', type: 'number', unit: 'px', default: '1' },
                    { key: 'borderColor', label: 'Color', type: 'color', default: '#E8E8E8' },
                    { key: 'borderStyle', label: 'Style', type: 'select', options: ['none', 'solid', 'dashed', 'dotted', 'double'], default: 'solid' }
                ]
            },
            'Effects': {
                expanded: false,
                properties: [
                    { key: 'boxShadow', label: 'Shadow', type: 'text', default: 'none' },
                    { key: 'filter', label: 'Filter', type: 'text', default: 'none' },
                    { key: 'backdropFilter', label: 'Backdrop Filter', type: 'text', default: 'none' },
                    { key: 'mixBlendMode', label: 'Mix Blend', type: 'select', options: ['normal', 'multiply', 'screen', 'overlay', 'darken', 'lighten'], default: 'normal' },
                    { key: 'transform', label: 'Transform', type: 'text', default: 'none' }
                ]
            },
            'Advanced': {
                expanded: false,
                properties: [
                    { key: 'cursor', label: 'Cursor', type: 'select', options: ['auto', 'pointer', 'grab', 'text', 'move', 'default', 'not-allowed'], default: 'auto' },
                    { key: 'pointerEvents', label: 'Pointer Events', type: 'select', options: ['auto', 'none'], default: 'auto' },
                    { key: 'visibility', label: 'Visibility', type: 'select', options: ['visible', 'hidden'], default: 'visible' },
                    { key: 'customCSS', label: 'Custom CSS', type: 'textarea', default: '' }
                ]
            }
        };
    }

    update() {
        const state = this.store.getState();
        const selectedIds = state.selectedElements;
        
        if (selectedIds.length === 0) {
            this.content.innerHTML = '<div class="no-selection">Select an element to edit its properties</div>';
            return;
        }
        
        const id = selectedIds[0];
        const element = state.elements.find(el => el.id === id);
        
        if (!element) {
            this.content.innerHTML = '<div class="no-selection">Element not found</div>';
            return;
        }
        
        this.selectedElement = element;
        this.renderProperties(element);
    }

    renderProperties(element) {
        this.content.innerHTML = '';
        
        Object.entries(this.propertyGroups).forEach(([groupName, group]) => {
            const groupDiv = document.createElement('div');
            groupDiv.className = 'property-group';
            
            // Header
            const header = document.createElement('div');
            header.className = 'property-group-header';
            header.innerHTML = `
                <span class="property-group-title">${groupName}</span>
                <span class="property-group-toggle">${group.expanded ? '−' : '+'}</span>
            `;
            header.addEventListener('click', () => {
                group.expanded = !group.expanded;
                this.renderProperties(element);
            });
            
            groupDiv.appendChild(header);
            
            // Content
            if (group.expanded) {
                const content = document.createElement('div');
                content.className = 'property-group-content';
                
                group.properties.forEach(prop => {
                    const control = this.createPropertyControl(prop, element);
                    content.appendChild(control);
                });
                
                groupDiv.appendChild(content);
            }
            
            this.content.appendChild(groupDiv);
        });
    }

    createPropertyControl(propConfig, element) {
        const div = document.createElement('div');
        div.className = 'property-control';
        
        const label = document.createElement('label');
        label.className = 'property-label';
        label.textContent = propConfig.label;
        
        const input = this.createInput(propConfig, element);
        
        div.appendChild(label);
        div.appendChild(input);
        
        return div;
    }

    createInput(propConfig, element) {
        const value = element.styles[propConfig.key] || propConfig.default;
        
        switch(propConfig.type) {
            case 'color':
                return this.createColorInput(propConfig, value, element);
            case 'number':
                return this.createNumberInput(propConfig, value, element);
            case 'select':
                return this.createSelectInput(propConfig, value, element);
            case 'text':
                return this.createTextInput(propConfig, value, element);
            case 'textarea':
                return this.createTextAreaInput(propConfig, value, element);
            default:
                return this.createTextInput(propConfig, value, element);
        }
    }

    createColorInput(propConfig, value, element) {
        const input = document.createElement('input');
        input.type = 'color';
        input.className = 'property-color';
        input.value = value || '#000000';
        
        input.addEventListener('change', (e) => {
            this.updateProperty(propConfig.key, e.target.value, element);
        });
        
        return input;
    }

    createNumberInput(propConfig, value, element) {
        const wrapper = document.createElement('div');
        wrapper.className = 'property-number-wrapper';
        
        const input = document.createElement('input');
        input.type = 'number';
        input.className = 'property-number';
        input.value = value || propConfig.default;
        
        if (propConfig.min !== undefined) input.min = propConfig.min;
        if (propConfig.max !== undefined) input.max = propConfig.max;
        if (propConfig.step !== undefined) input.step = propConfig.step;
        
        const slider = document.createElement('input');
        slider.type = 'range';
        slider.className = 'property-slider';
        slider.min = propConfig.min || 0;
        slider.max = propConfig.max || 100;
        slider.step = propConfig.step || 1;
        slider.value = value || propConfig.default;
        
        input.addEventListener('change', (e) => {
            const val = e.target.value;
            slider.value = val;
            this.updateProperty(propConfig.key, val, element);
        });
        
        slider.addEventListener('input', (e) => {
            const val = e.target.value;
            input.value = val;
            this.updateProperty(propConfig.key, val, element);
        });
        
        wrapper.appendChild(input);
        wrapper.appendChild(slider);
        
        return wrapper;
    }

    createSelectInput(propConfig, value, element) {
        const select = document.createElement('select');
        select.className = 'property-select';
        
        propConfig.options.forEach(option => {
            const opt = document.createElement('option');
            opt.value = option;
            opt.textContent = option.charAt(0).toUpperCase() + option.slice(1);
            if (option === value || option === propConfig.default) {
                opt.selected = true;
            }
            select.appendChild(opt);
        });
        
        select.addEventListener('change', (e) => {
            this.updateProperty(propConfig.key, e.target.value, element);
        });
        
        return select;
    }

    createTextInput(propConfig, value, element) {
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'property-text';
        input.value = value || propConfig.default || '';
        
        input.addEventListener('change', (e) => {
            this.updateProperty(propConfig.key, e.target.value, element);
        });
        
        return input;
    }

    createTextAreaInput(propConfig, value, element) {
        const textarea = document.createElement('textarea');
        textarea.className = 'property-textarea';
        textarea.value = value || propConfig.default || '';
        textarea.rows = 4;
        
        textarea.addEventListener('change', (e) => {
            this.updateProperty(propConfig.key, e.target.value, element);
        });
        
        return textarea;
    }

    updateProperty(key, value, element) {
        const styles = { ...element.styles, [key]: value };
        this.store.updateElement(element.id, { styles });
    }
}
