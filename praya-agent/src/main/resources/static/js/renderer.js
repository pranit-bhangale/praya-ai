const Renderer = (function() {
    'use strict';

    let messagesContainer = null;
    let welcomeMessage = null;
    let streamingIndicator = null;

    let currentAssistantMessage = null;
    let currentToolCalls = new Map(); // toolId -> toolCallElement
    let messageCounter = 0;

    function init() {
        messagesContainer = document.getElementById('messagesContainer');
        welcomeMessage = document.getElementById('welcomeMessage');
        streamingIndicator = document.getElementById('streamingIndicator');
        renderSuggestedPrompts();
    }

    function hideWelcome() {
        if (welcomeMessage && !welcomeMessage.hidden) {
            welcomeMessage.hidden = true;
        }
    }

    function showWelcome() {
        if (welcomeMessage && welcomeMessage.hidden) {
            welcomeMessage.hidden = false;
        }
        clearMessages();
    }

    function clearMessages() {
        if (messagesContainer) {
            const messages = messagesContainer.querySelectorAll('.message');
            messages.forEach(msg => msg.remove());
        }
        currentAssistantMessage = null;
        currentToolCalls.clear();
        messageCounter = 0;
    }

    function renderUserMessage(content) {
        hideWelcome();
        
        const messageEl = createMessageElement('user', content);
        appendMessage(messageEl);
        scrollToBottom();
        
        return messageEl;
    }

    function startAssistantMessage() {
        hideWelcome();
        
        currentAssistantMessage = createMessageElement('assistant', '', true);
        currentAssistantMessage.classList.add('streaming');
        appendMessage(currentAssistantMessage);
        
        const toolCallsContainer = document.createElement('div');
        toolCallsContainer.className = 'tool-calls-container';
        currentAssistantMessage.querySelector('.message-content').appendChild(toolCallsContainer);
        
        currentToolCalls.clear();
        scrollToBottom();
        
        return currentAssistantMessage;
    }

    function appendAssistantToken(token) {
        if (!currentAssistantMessage) return;
        
        const body = currentAssistantMessage.querySelector('.message-body');
        if (body) {
            body.textContent += token;
            scrollToBottom();
        }
    }

    function completeAssistantMessage() {
        if (!currentAssistantMessage) return;
        
        currentAssistantMessage.classList.remove('streaming');
        currentAssistantMessage = null;
        scrollToBottom();
    }

    function setAssistantMessageContent(content) {
        if (!currentAssistantMessage) return;
        
        const body = currentAssistantMessage.querySelector('.message-body');
        if (body) {
            body.innerHTML = renderMarkdown(content);
        }
    }

    function renderToolCallStarted(toolCallEvent) {
        if (!currentAssistantMessage) return;
        
        const toolCallsContainer = currentAssistantMessage.querySelector('.tool-calls-container');
        if (!toolCallsContainer) return;
        
        const toolCallEl = createToolCallElement(toolCallEvent);
        toolCallsContainer.appendChild(toolCallEl);
        currentToolCalls.set(toolCallEvent.id, toolCallEl);
        
        scrollToBottom();
    }

    function renderToolCallCompleted(toolCallEvent) {
        const toolCallEl = currentToolCalls.get(toolCallEvent.id);
        if (!toolCallEl) return;
        
        toolCallEl.classList.remove('started');
        toolCallEl.classList.add('completed');
        
        const statusBadge = toolCallEl.querySelector('.status-badge');
        if (statusBadge) {
            statusBadge.className = 'status-badge completed';
            statusBadge.innerHTML = '<span class="status-dot"></span>Completed';
        }
        
        const icon = toolCallEl.querySelector('.tool-call-icon svg');
        if (icon) {
            icon.innerHTML = '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>';
        }
        
        if (toolCallEvent.responseData) {
            addToolCallResponse(toolCallEl, toolCallEvent.responseData);
        }
        
        scrollToBottom();
    }

    function renderToolCallError(toolCallEvent) {
        const toolCallEl = currentToolCalls.get(toolCallEvent.id);
        if (!toolCallEl) return;
        
        toolCallEl.classList.remove('started');
        toolCallEl.classList.add('completed');
        
        const statusBadge = toolCallEl.querySelector('.status-badge');
        if (statusBadge) {
            statusBadge.className = 'status-badge error';
            statusBadge.innerHTML = '<span class="status-dot"></span>Error';
        }
        
        if (toolCallEvent.responseData) {
            addToolCallResponse(toolCallEl, toolCallEvent.responseData, true);
        }
    }

    function renderErrorMessage(error) {
        hideWelcome();
        
        const messageEl = createMessageElement('assistant', error, false);
        messageEl.classList.add('error');
        const avatar = messageEl.querySelector('.message-avatar');
        if (avatar) {
            avatar.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>';
        }
        appendMessage(messageEl);
        scrollToBottom();
    }

    function showStreamingIndicator(text) {
        if (streamingIndicator) {
            const textEl = streamingIndicator.querySelector('.streaming-text');
            if (textEl && text) {
                textEl.textContent = text;
            }
            streamingIndicator.hidden = false;
            scrollToBottom();
        }
    }

    function hideStreamingIndicator() {
        if (streamingIndicator) {
            streamingIndicator.hidden = true;
        }
    }

    function createMessageElement(role, content, isStreaming = false) {
        const messageEl = document.createElement('div');
        messageEl.className = `message ${role}`;
        messageEl.dataset.messageId = ++messageCounter;
        
        const avatar = createAvatar(role);
        const contentEl = document.createElement('div');
        contentEl.className = 'message-content';
        
        const header = document.createElement('div');
        header.className = 'message-header';
        header.innerHTML = `
            <span class="message-role">${role === 'user' ? 'You' : 'Praya AI'}</span>
            <span class="message-time">${formatTime(new Date())}</span>
        `;
        
        const body = document.createElement('div');
        body.className = 'message-body';
        if (isStreaming) {
            body.textContent = '';
        } else {
            body.innerHTML = renderMarkdown(content);
        }
        
        contentEl.appendChild(header);
        contentEl.appendChild(body);
        
        if (role === 'user') {
            messageEl.appendChild(contentEl);
            messageEl.appendChild(avatar);
        } else {
            messageEl.appendChild(avatar);
            messageEl.appendChild(contentEl);
        }
        
        return messageEl;
    }

    function createAvatar(role) {
        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        
        if (role === 'user') {
            avatar.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>';
        } else {
            avatar.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>';
        }
        
        return avatar;
    }

    function createToolCallElement(toolCallEvent) {
        const toolCallEl = document.createElement('div');
        toolCallEl.className = 'tool-call started';
        toolCallEl.dataset.toolId = toolCallEvent.id;
        
        const header = document.createElement('div');
        header.className = 'tool-call-header';
        
        const toggle = document.createElement('button');
        toggle.className = 'tool-call-toggle';
        toggle.setAttribute('aria-label', 'Expand tool details');
        toggle.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>';
        
        const icon = document.createElement('div');
        icon.className = 'tool-call-icon';
        icon.innerHTML = getToolIcon(toolCallEvent.name);
        
        const info = document.createElement('div');
        info.className = 'tool-call-info';
        info.innerHTML = `
            <div class="tool-call-name">${escapeHtml(toolCallEvent.name)}</div>
            <div class="tool-call-status">
                <span class="status-badge started"><span class="status-dot"></span>Running</span>
                <span class="tool-call-timestamp">${formatTime(new Date())}</span>
            </div>
        `;
        
        const actions = document.createElement('div');
        actions.className = 'tool-call-actions';
        actions.innerHTML = `
            <button class="tool-call-action-btn expand-btn" aria-label="View full details" title="View full details">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
            </button>
        `;
        
        header.appendChild(toggle);
        header.appendChild(icon);
        header.appendChild(info);
        header.appendChild(actions);
        
        const body = document.createElement('div');
        body.className = 'tool-call-body';
        
        toolCallEl.appendChild(header);
        toolCallEl.appendChild(body);
        
        header.addEventListener('click', (e) => {
            if (!e.target.closest('.tool-call-action-btn') && !e.target.closest('.tool-call-toggle')) {
                toggleToolCall(toolCallEl);
            }
        });
        
        toggle.addEventListener('click', (e) => {
            e.stopPropagation();
            toggleToolCall(toolCallEl);
        });
        
        actions.querySelector('.expand-btn').addEventListener('click', (e) => {
            e.stopPropagation();
            ToolCalling.openToolDetailModal(toolCallEvent);
        });
        
        return toolCallEl;
    }

    function addToolCallResponse(toolCallEl, responseData, isError = false) {
        const body = toolCallEl.querySelector('.tool-call-body');
        if (!body) return;
        
        const responseSection = document.createElement('div');
        responseSection.className = 'tool-call-response';
        responseSection.innerHTML = `
            <div class="tool-call-section-label">Response</div>
            <pre class="tool-call-json"><code>${escapeHtml(formatJson(responseData))}</code></pre>
        `;
        
        body.appendChild(responseSection);
    }

    function toggleToolCall(toolCallEl) {
        const isMinimized = toolCallEl.classList.toggle('minimized');
        const toggleBtn = toolCallEl.querySelector('.tool-call-toggle');
        if (toggleBtn) {
            toggleBtn.setAttribute('aria-label', isMinimized ? 'Expand tool details' : 'Collapse tool details');
        }
    }

    function appendMessage(messageEl) {
        if (messagesContainer) {
            if (streamingIndicator && !streamingIndicator.hidden) {
                messagesContainer.insertBefore(messageEl, streamingIndicator);
            } else {
                messagesContainer.appendChild(messageEl);
            }
        }
    }

    function scrollToBottom() {
        if (messagesContainer) {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }
    }

    function renderSuggestedPrompts() {
        const container = document.getElementById('suggestedPrompts');
        if (!container) return;
        
        const prompts = [
            'What projects have you worked on?',
            'Show me your GitHub repositories',
            'Tell me about your experience',
            'What technologies do you know?',
            'Share your latest blog posts',
            'What certifications do you have?'
        ];
        
        container.innerHTML = prompts.map(prompt => 
            `<button class="suggested-prompt" data-prompt="${escapeHtml(prompt)}">${escapeHtml(prompt)}</button>`
        ).join('');
        
        container.querySelectorAll('.suggested-prompt').forEach(btn => {
            btn.addEventListener('click', () => {
                const prompt = btn.dataset.prompt;
                if (window.App && window.App.handleUserPrompt) {
                    window.App.handleUserPrompt(prompt);
                }
            });
        });
    }

    function formatTime(date) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

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
            'github': '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23A11.509 11.509 0 0112 5.803c1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.566 21.797 24 17.3 24 12c0-6.627-5.373-12-12-12z"/></svg>',
            'linkedin': '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/></svg>',
            'search': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>',
        };
        const lowerName = toolName.toLowerCase();
        for (const [key, icon] of Object.entries(icons)) {
            if (lowerName.includes(key)) {
                return icon;
            }
        }
        return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>';
    }
    function renderMarkdown(text) {
        if (!text) return '';
        let html = escapeHtml(text);
        html = html.replace(/```(\w+)?\n([\s\S]*?)```/g, (match, lang, code) => {
            return `<pre><code class="language-${lang || ''}">${code}</code></pre>`;
        });
        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
        html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        html = html.replace(/^### (.*$)/gm, '<h3>$1</h3>');
        html = html.replace(/^## (.*$)/gm, '<h2>$1</h2>');
        html = html.replace(/^# (.*$)/gm, '<h1>$1</h1>');
        html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
        html = html.replace(/^> (.*$)/gm, '<blockquote>$1</blockquote>');
        html = html.replace(/^\- (.*$)/gm, '<li>$1</li>');
        html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');
        html = html.replace(/^\d+\. (.*$)/gm, '<li>$1</li>');
        html = html.replace(/\n\n/g, '</p><p>');
        html = '<p>' + html + '</p>';
        html = html.replace(/<p><\/p>/g, '');
        html = html.replace(/<p>(<pre>)/g, '$1');
        html = html.replace(/(<\/pre>)<\/p>/g, '$1');
        html = html.replace(/<p>(<ul>)/g, '$1');
        html = html.replace(/<\/ul><\/p>/g, '</ul>');
        html = html.replace(/<p>(<blockquote>)/g, '$1');
        html = html.replace(/<\/blockquote><\/p>/g, '</blockquote>');
        html = html.replace(/<p>(<h[1-3]>)/g, '$1');
        html = html.replace(/(<\/h[1-3]>)<\/p>/g, '$1');
        
        return html;
    }

    return {
        init,
        hideWelcome,
        showWelcome,
        clearMessages,
        renderUserMessage,
        startAssistantMessage,
        appendAssistantToken,
        completeAssistantMessage,
        setAssistantMessageContent,
        renderToolCallStarted,
        renderToolCallCompleted,
        renderToolCallError,
        renderErrorMessage,
        showStreamingIndicator,
        hideStreamingIndicator,
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    Renderer.init();
});

window.Renderer = Renderer;