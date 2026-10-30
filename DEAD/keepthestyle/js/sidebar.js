/**
 * KeepTheStyle - Sidebar Management
 * Manages the left sidebar with elements and categories
 */

class SidebarManager {
    constructor(container) {
        this.container = container;
        this.nav = container.querySelector('.sidebar-nav');
        this.elementsList = container.querySelector('.sidebar-elements');
        this.searchInput = container.querySelector('.sidebar-search input');
        
        this.categories = window.getCategoriesWithElements();
        this.currentCategory = Object.keys(this.categories)[0];
        
        this.init();
    }

    init() {
        this.renderCategories();
        this.renderElements();
        this.bindEvents();
    }

    renderCategories() {
        this.nav.innerHTML = '';
        Object.keys(this.categories).forEach(name => {
            const btn = document.createElement('button');
            btn.className = `category-btn ${name === this.currentCategory ? 'active' : ''}`;
            btn.textContent = name;
            btn.dataset.category = name;
            btn.addEventListener('click', () => {
                this.currentCategory = name;
                this.renderCategories();
                this.renderElements();
            });
            this.nav.appendChild(btn);
        });
    }

    renderElements(filter = '') {
        const category = this.categories[this.currentCategory];
        const elements = category.elements;
        
        this.elementsList.innerHTML = '';
        elements.forEach(el => {
            const item = this.createElementItem(el);
            if (filter && !el.name.toLowerCase().includes(filter)) {
                item.style.display = 'none';
            }
            this.elementsList.appendChild(item);
        });
    }

    createElementItem(element) {
        const div = document.createElement('div');
        div.className = 'element-item';
        div.draggable = true;
        div.dataset.tag = element.tag;
        
        const icon = document.createElement('span');
        icon.className = 'element-icon';
        icon.textContent = element.icon || '⊞';
        
        const name = document.createElement('span');
        name.className = 'element-name';
        name.textContent = element.name;
        
        div.appendChild(icon);
        div.appendChild(name);
        
        // Double click to insert
        div.addEventListener('dblclick', () => {
            this.insertElement(element);
        });
        
        // Drag and drop
        div.addEventListener('dragstart', (e) => {
            e.dataTransfer.effectAllowed = 'copy';
            e.dataTransfer.setData('text/plain', JSON.stringify({
                tag: element.tag,
                defaults: element.defaults
            }));
        });
        
        return div;
    }

    insertElement(element) {
        const defaults = element.defaults || {};
        const newElement = {
            tag: element.tag,
            position: { x: 100 + Math.random() * 100, y: 100 + Math.random() * 100 },
            size: { width: 200, height: 150 },
            styles: defaults.styles || {},
            content: defaults.content || '',
            attributes: defaults.attributes || {}
        };
        
        window.store.addElement(newElement);
    }

    bindEvents() {
        // Search functionality
        this.searchInput.addEventListener('input', (e) => {
            const query = e.target.value.toLowerCase();
            this.renderElements(query);
        });
    }
}
