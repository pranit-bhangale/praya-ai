const App = (function() {
    'use strict';

    let promptInput = null;
    let sendBtn = null;
    let newChatBtn = null;
    let attachBtn = null;

    let isProcessing = false;
    let currentConversationId = generateConversationId();
    let messageHistory = [];

    function init() {
        promptInput = document.getElementById('promptInput');
        sendBtn = document.getElementById('sendBtn');
        newChatBtn = document.getElementById('newChatBtn');
        attachBtn = document.getElementById('attachBtn');

        Streaming.init({
            callbacks: {
                onToken: handleToken,
                onToolStarted: handleToolStarted,
                onToolCompleted: handleToolCompleted,
                onError: handleError,
                onDone: handleDone,
                onConnect: handleConnect,
                onDisconnect: handleDisconnect,
            }
        });
        bindEvents();
        setupAutoResize();
        loadConversation();
    }

    function bindEvents() {
        sendBtn?.addEventListener('click', handleSend);
        promptInput?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
            }
        });
        newChatBtn?.addEventListener('click', handleNewChat);
        attachBtn?.addEventListener('click', handleAttach);
        document.addEventListener('click', (e) => {
            const promptBtn = e.target.closest('.suggested-prompt');
            if (promptBtn && promptBtn.dataset.prompt) {
                handleUserPrompt(promptBtn.dataset.prompt);
            }
        });
    }

    function setupAutoResize() {
        if (!promptInput) return;
        promptInput.addEventListener('input', () => {
            promptInput.style.height = 'auto';
            promptInput.style.height = Math.min(promptInput.scrollHeight, 200) + 'px';
        });
    }

    async function handleSend() {
        const prompt = promptInput?.value.trim();
        if (!prompt || isProcessing) return;
        await sendUserMessage(prompt);
    }

    async function handleUserPrompt(prompt) {
        if (isProcessing) return;
        if (promptInput) {
            promptInput.value = prompt;
            promptInput.style.height = 'auto';
        }
        await sendUserMessage(prompt);
    }

    async function sendUserMessage(prompt) {
        if (isProcessing) return;
        isProcessing = true;
        updateUIState();
        Renderer.renderUserMessage(prompt);
        saveMessage('user', prompt);
        if (promptInput) {
            promptInput.value = '';
            promptInput.style.height = 'auto';
        }
        Renderer.startAssistantMessage();
        Renderer.showStreamingIndicator('AI is thinking...');
        try {
            await Streaming.sendMessage(prompt, currentConversationId);
        } catch (error) {
            console.error('Send message error:', error);
        }
    }

    function handleNewChat() {
        if (isProcessing) return;
        currentConversationId = generateConversationId();
        messageHistory = [];
        Renderer.clearMessages();
        Renderer.showWelcome();
        clearConversationStorage();
        updateUIState();
    }

    function handleAttach() {
        console.log('Attach file clicked');
    }

    function handleToken(token) {
        Renderer.hideStreamingIndicator();
        Renderer.appendAssistantToken(token);
    }

    function handleToolStarted(toolEvent) {
        Renderer.hideStreamingIndicator();
        Renderer.renderToolCallStarted(toolEvent);
        ToolCalling.minimizeAllToolCalls(toolEvent.id);
    }

    function handleToolCompleted(toolEvent) {
        Renderer.renderToolCallCompleted(toolEvent);
    }

    function handleError(error) {
        Renderer.hideStreamingIndicator();
        Renderer.renderErrorMessage(error instanceof Error ? error.message : String(error));
        isProcessing = false;
        updateUIState();
    }

    function handleDone() {
        Renderer.hideStreamingIndicator();
        Renderer.completeAssistantMessage();
        isProcessing = false;
        updateUIState();
    }

    function handleConnect() {
        console.log('Streaming connected');
    }

    function handleDisconnect() {
        console.log('Streaming disconnected');
    }

    function updateUIState() {
        if (sendBtn) {
            sendBtn.disabled = isProcessing || !promptInput?.value.trim();
        }
        
        if (promptInput) {
            promptInput.disabled = isProcessing;
        }
        
        if (newChatBtn) {
            newChatBtn.disabled = isProcessing;
        }
    }

    function saveMessage(role, content) {
        messageHistory.push({
            role,
            content,
            timestamp: Date.now(),
        });
        saveConversation();
    }

    function saveConversation() {
        try {
            const data = {
                conversationId: currentConversationId,
                messages: messageHistory,
                updatedAt: Date.now(),
            };
            localStorage.setItem('praya_conversation', JSON.stringify(data));
        } catch (error) {
            console.warn('Failed to save conversation:', error);
        }
    }

    function loadConversation() {
        try {
            const data = localStorage.getItem('praya_conversation');
            if (data) {
                const parsed = JSON.parse(data);
                if (parsed.conversationId && parsed.messages) {
                    currentConversationId = parsed.conversationId;
                    messageHistory = parsed.messages;
                    messageHistory.forEach(msg => {
                        if (msg.role === 'user') {
                            Renderer.renderUserMessage(msg.content);
                        } else {
                            Renderer.startAssistantMessage();
                            Renderer.setAssistantMessageContent(msg.content);
                            Renderer.completeAssistantMessage();
                        }
                    });
                    setTimeout(() => {
                        const container = document.getElementById('messagesContainer');
                        if (container) container.scrollTop = container.scrollHeight;
                    }, 0);
                }
            }
        } catch (error) {
            console.warn('Failed to load conversation:', error);
        }
    }

    function clearConversationStorage() {
        try {
            localStorage.removeItem('praya_conversation');
        } catch (error) {
            console.warn('Failed to clear conversation:', error);
        }
    }

    function generateConversationId() {
        return 'conv_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    return {
        init,
        handleUserPrompt,
        handleSend,
        handleNewChat,
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    App.init();
});

window.App = App;