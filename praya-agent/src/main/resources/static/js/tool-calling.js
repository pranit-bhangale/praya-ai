/**
 * Tool Calling Module - Handles tool call UI interactions
 * Minimize/maximize, detail modal, and tool call state management
 */

const ToolCalling = (function() {
    'use strict';

    // DOM references
    let modalOverlay = null;
    let modalBody = null;
    let modalTitle = null;
    let modalClose = null;

    // State
    let currentToolData = null;

    // ========================================
    // Initialization
    // ========================================
    function init() {
        modalOverlay = document.getElementById('toolDetailModal');
        modalBody = document.getElementById('modalBody');
        modalTitle = document.getElementById('modalTitle');
        modalClose = document.getElementById('modalClose');

        if (modalClose) {
            modalClose.addEventListener('click', closeModal);
        }

        if (modalOverlay) {
            modalOverlay.addEventListener('click', (e) => {
                if (e.target === modalOverlay) {
                    closeModal();
                }
            });
        }

        // ESC key to close modal
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && modalOverlay && !modalOverlay.hidden) {
                closeModal();
            }
        });
    }

    // ========================================
    // Public API
    // ========================================
    function openToolDetailModal(toolCallEvent) {
        currentToolData = toolCallEvent;
        
        if (!modalOverlay || !modalBody || !modalTitle) return;

        // Update title
        modalTitle.textContent = `${toolCallEvent.name} ${toolCallEvent.phase === 'STARTED' ? '(Running)' : '(Completed)'}`;

        // Build modal content
        let content = '';

        // Tool info header
        content += `
            <div class="tool-detail-header">
                <div class="tool-detail-icon">${getToolIcon(toolCallEvent.name)}</div>
                <div class="tool-detail-info">
                    <h4>${escapeHtml(toolCallEvent.name)}</h4>
                    <div class="tool-detail-meta">
                        <span class="status-badge ${toolCallEvent.phase.toLowerCase()}">${toolCallEvent.phase}</span>
                        <span class="tool-detail-id">ID: ${escapeHtml(toolCallEvent.id)}</span>
                    </div>
                </div>
            </div>
        `;

        // Response data (if completed)
        if (toolCallEvent.phase === 'COMPLETED' && toolCallEvent.responseData) {
            content += `
                <div class="tool-detail-section">
                    <h5>Response Data</h5>
                    <pre class="tool-call-json"><code>${escapeHtml(formatJson(toolCallEvent.responseData))}</code></pre>
                </div>
            `;
        } else if (toolCallEvent.phase === 'STARTED') {
            content += `
                <div class="tool-detail-section">
                    <h5>Status</h5>
                    <p class="tool-detail-status">Tool is currently executing...</p>
                    <div class="streaming-dots" style="justify-content: flex-start; margin-top: 8px;">
                        <span></span><span></span><span></span>
                    </div>
                </div>
            `;
        }

        modalBody.innerHTML = content;
        modalOverlay.hidden = false;
        
        // Focus trap
        setTimeout(() => modalClose?.focus(), 10);
    }

    function closeModal() {
        if (modalOverlay) {
            modalOverlay.hidden = true;
            currentToolData = null;
        }
    }

    function minimizeAllToolCalls(exceptId = null) {
        const toolCalls = document.querySelectorAll('.tool-call');
        toolCalls.forEach(toolCall => {
            if (toolCall.dataset.toolId !== exceptId) {
                toolCall.classList.add('minimized');
            }
        });
    }

    function maximizeToolCall(toolId) {
        const toolCall = document.querySelector(`.tool-call[data-tool-id="${toolId}"]`);
        if (toolCall) {
            toolCall.classList.remove('minimized');
        }
    }

    function toggleToolCall(toolId) {
        const toolCall = document.querySelector(`.tool-call[data-tool-id="${toolId}"]`);
        if (toolCall) {
            toolCall.classList.toggle('minimized');
        }
    }

    // ========================================
    // Utility Functions
    // ========================================
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatJson(json) {
        try {
            const parsed = typeof json === 'string' ? JSON.parse(json) : json;
            return JSON.stringify(parsed, null, 2);
        } catch {
            return escapeHtml(String(json));
        }
    }

    function getToolIcon(toolName) {
        const icons = {
            'github': '<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23A11.509 11.509 0 0112 5.803c1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.566 21.797 24 17.3 24 12c0-6.627-5.373-12-12-12z"/></svg>',
            'linkedin': '<svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/></svg>',
            'search': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>',
        };

        const lowerName = toolName.toLowerCase();
        for (const [key, icon] of Object.entries(icons)) {
            if (lowerName.includes(key)) {
                return icon;
            }
        }

        return '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>';
    }

    // ========================================
    // Public API Export
    // ========================================
    return {
        init,
        openToolDetailModal,
        closeModal,
        minimizeAllToolCalls,
        maximizeToolCall,
        toggleToolCall,
    };
})();

// Auto-initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    ToolCalling.init();
});

// Export for global access
window.ToolCalling = ToolCalling;