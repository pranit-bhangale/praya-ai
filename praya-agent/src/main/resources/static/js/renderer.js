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
            messages.forEach(msg => {
                const toolCalls = msg.querySelectorAll('.tool-call');
                toolCalls.forEach(tc => {
                    if (tc._timer) clearInterval(tc._timer);
                });
                msg.remove();
            });
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
        
        if (toolCallEl._timer) {
            clearInterval(toolCallEl._timer);
            toolCallEl._timer = null;
        }
        
        const sec = toolCallEl._startTime ? ((Date.now() - toolCallEl._startTime) / 1000).toFixed(1) + 's' : '';
        toolCallEl._event = toolCallEvent;
        
        toolCallEl.classList.remove('started');
        toolCallEl.classList.add('completed');
        
        toolCallEl.innerHTML = `
            <div class="tool-call-pill">
                <span class="tool-call-check">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <polyline points="20 6 9 17 4 12"/>
                    </svg>
                </span>
                <span class="tool-call-label">Used tool <strong>${escapeHtml(toolCallEvent.name)}</strong></span>
                ${sec ? `<span class="tool-call-duration">${sec}</span>` : ''}
                <span class="tool-call-chevron">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <polyline points="9 18 15 12 9 6"/>
                    </svg>
                </span>
            </div>
        `;
        
        toolCallEl.onclick = () => ToolCalling.openToolDetailModal(toolCallEvent);
        scrollToBottom();
    }

    function renderToolCallError(toolCallEvent) {
        const toolCallEl = currentToolCalls.get(toolCallEvent.id);
        if (!toolCallEl) return;
        
        if (toolCallEl._timer) {
            clearInterval(toolCallEl._timer);
            toolCallEl._timer = null;
        }
        
        toolCallEl._event = toolCallEvent;
        toolCallEl.classList.remove('started');
        toolCallEl.classList.add('error');
        
        toolCallEl.innerHTML = `
            <div class="tool-call-pill error">
                <span class="tool-call-error-icon">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                </span>
                <span class="tool-call-label">Tool error: <strong>${escapeHtml(toolCallEvent.name)}</strong></span>
                <span class="tool-call-chevron">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <polyline points="9 18 15 12 9 6"/>
                    </svg>
                </span>
            </div>
        `;
        
        toolCallEl.onclick = () => ToolCalling.openToolDetailModal(toolCallEvent);
        scrollToBottom();
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
        if (role === 'assistant') {
            const toolCallsContainer = document.createElement('div');
            toolCallsContainer.className = 'tool-calls-container';
            contentEl.appendChild(toolCallsContainer);
        }
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
        toolCallEl._startTime = Date.now();
        toolCallEl._event = toolCallEvent;
        
        toolCallEl.innerHTML = `
            <div class="tool-call-pill">
                <span class="tool-call-spinner"></span>
                <span class="tool-call-label">Calling tool <strong>${escapeHtml(toolCallEvent.name)}</strong>...</span>
                <span class="tool-call-duration">0s</span>
                <span class="tool-call-chevron">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <polyline points="9 18 15 12 9 6"/>
                    </svg>
                </span>
            </div>
        `;
        
        toolCallEl._timer = setInterval(() => {
            const durationEl = toolCallEl.querySelector('.tool-call-duration');
            if (durationEl && toolCallEl._startTime) {
                const sec = Math.floor((Date.now() - toolCallEl._startTime) / 1000);
                durationEl.textContent = sec < 60 ? `${sec}s` : `${Math.floor(sec / 60)}m ${sec % 60}s`;
            }
        }, 1000);
        
        toolCallEl.addEventListener('click', () => {
            ToolCalling.openToolDetailModal(toolCallEl._event || toolCallEvent);
        });
        
        return toolCallEl;
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