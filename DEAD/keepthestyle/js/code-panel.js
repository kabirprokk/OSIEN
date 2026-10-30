/**
 * KeepTheStyle - Code Panel
 * Generates and displays HTML, CSS, and JavaScript code
 */

class CodePanel {
    constructor(container) {
        this.container = container;
        this.codeDisplay = container.querySelector('code');
        this.store = window.store;
        this.currentTab = 'html';
        
        this.bindEvents();
        this.subscribe();
        this.generateCode();
    }

    bindEvents() {
        // Tab switching
        this.container.querySelectorAll('.code-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                this.currentTab = tab.dataset.tab;
                this.container.querySelectorAll('.code-tab').forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
                this.generateCode();
            });
        });
        
        // Copy button
        this.container.querySelector('.code-btn[title="Copy"]').addEventListener('click', () => {
            this.copyCode();
        });
        
        // Download button
        this.container.querySelector('.code-btn[title="Download"]').addEventListener('click', () => {
            this.downloadCode();
        });
        
        // Clear button
        this.container.querySelector('.code-btn[title="Clear"]').addEventListener('click', () => {
            this.clearCode();
        });
        
        // Format button
        this.container.querySelector('.code-btn[title="Format"]').addEventListener('click', () => {
            this.formatCode();
        });
    }

    subscribe() {
        this.store.subscribe(() => {
            this.generateCode();
        });
    }

    generateCode() {
        const state = this.store.getState();
        const elements = state.elements;
        
        let code = '';
        switch(this.currentTab) {
            case 'html':
                code = this.generateHTML(elements);
                break;
            case 'css':
                code = this.generateCSS(elements);
                break;
            case 'javascript':
                code = this.generateJavaScript(elements);
                break;
        }
        
        this.codeDisplay.textContent = code;
        this.highlightCode();
    }

    generateHTML(elements) {
        let html = `<!DOCTYPE html>\n<html lang="en">\n<head>\n`;
        html += `    <meta charset="UTF-8">\n`;
        html += `    <meta name="viewport" content="width=device-width, initial-scale=1.0">\n`;
        html += `    <title>${this.store.getState().projectName}</title>\n`;
        html += `    <link rel="stylesheet" href="styles.css">\n`;
        html += `</head>\n<body>\n`;
        
        elements.forEach(el => {
            html += this.renderElementHTML(el, 1);
        });
        
        html += `</body>\n</html>`;
        return html;
    }

    renderElementHTML(element, indent) {
        const spaces = '    '.repeat(indent);
        const tag = element.tag || 'div';
        const content = element.content || '';
        const attributes = this.renderAttributes(element.attributes || {});
        const styles = this.renderInlineStyles(element.styles || {});
        
        let html = `${spaces}<${tag}${attributes} style="${styles}">`;
        
        if (content) {
            html += content;
        }
        
        // Render children
        if (element.children && element.children.length > 0) {
            html += '\n';
            element.children.forEach(child => {
                html += this.renderElementHTML(child, indent + 1);
            });
            html += spaces;
        }
        
        html += `</${tag}>\n`;
        return html;
    }

    renderAttributes(attributes) {
        let html = '';
        Object.entries(attributes).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                html += ` ${key}="${value}"`;
            }
        });
        return html;
    }

    renderInlineStyles(styles) {
        let css = '';
        Object.entries(styles).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                css += `${key}: ${value}; `;
            }
        });
        return css;
    }

    generateCSS(elements) {
        let css = `/* KeepTheStyle Generated CSS */\n\n`;
        css += `* {\n`;
        css += `    margin: 0;\n`;
        css += `    padding: 0;\n`;
        css += `    box-sizing: border-box;\n`;
        css += `}\n\n`;
        
        elements.forEach(el => {
            const selector = this.getSelector(el);
            const styles = el.styles || {};
            
            if (Object.keys(styles).length > 0) {
                css += `${selector} {\n`;
                Object.entries(styles).forEach(([key, value]) => {
                    if (value !== undefined && value !== null && value !== '') {
                        css += `    ${key}: ${value};\n`;
                    }
                });
                css += `}\n\n`;
            }
        });
        
        return css;
    }

    getSelector(element) {
        // Generate a unique selector based on position
        const id = element.id || `el-${Math.random().toString(36).substr(2, 6)}`;
        return `#${id}`;
    }

    generateJavaScript(elements) {
        let js = `// KeepTheStyle Generated JavaScript\n\n`;
        js += `// Your JavaScript code goes here\n\n`;
        js += `document.addEventListener('DOMContentLoaded', function() {\n`;
        js += `    console.log('Page loaded successfully!');\n`;
        js += `});\n`;
        return js;
    }

    highlightCode() {
        // Simple syntax highlighting - can be enhanced with a library
        const code = this.codeDisplay.textContent;
        // Apply basic formatting
        this.codeDisplay.innerHTML = this.syntaxHighlight(code);
    }

    syntaxHighlight(code) {
        // Simple highlighting for demo
        return code
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/(&lt;\/?[a-z][a-z0-9]*\s*&gt;)/gi, '<span class="hl-tag">$1</span>')
            .replace(/(&lt;!--.*?--&gt;)/g, '<span class="hl-comment">$1</span>')
            .replace(/(&quot;.*?&quot;)/g, '<span class="hl-string">$1</span>');
    }

    copyCode() {
        const code = this.codeDisplay.textContent;
        navigator.clipboard.writeText(code).then(() => {
            this.showNotification('Code copied to clipboard!');
        }).catch(() => {
            // Fallback
            const textarea = document.createElement('textarea');
            textarea.value = code;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
            this.showNotification('Code copied to clipboard!');
        });
    }

    downloadCode() {
        const code = this.codeDisplay.textContent;
        const extension = this.getFileExtension();
        const filename = `${this.store.getState().projectName}.${extension}`;
        
        const blob = new Blob([code], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    getFileExtension() {
        switch(this.currentTab) {
            case 'html': return 'html';
            case 'css': return 'css';
            case 'javascript': return 'js';
            default: return 'txt';
        }
    }

    clearCode() {
        // Only clear if the user confirms
        if (confirm('Are you sure you want to clear all code?')) {
            this.store.setState({
                elements: []
            });
            this.generateCode();
        }
    }

    formatCode() {
        // Simple formatting - can be enhanced with a proper formatter
        const code = this.codeDisplay.textContent;
        const formatted = code
            .replace(/\n\s*\n/g, '\n\n')
            .replace(/\{\s*\}/g, '{}')
            .trim();
        this.codeDisplay.textContent = formatted;
        this.highlightCode();
        this.showNotification('Code formatted!');
    }

    showNotification(message) {
        // Simple notification system
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

// Add keyframe animation for notifications
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
`;
document.head.appendChild(style);
