/**
 * Streaming Module - Handles SSE (Server-Sent Events) connection and parsing
 * Manages connection lifecycle, event parsing, and reconnection logic
 */

const Streaming = (function() {
    'use strict';

    // Configuration
    const CONFIG = {
        endpoint: '/api/agent/chat',
        reconnectDelay: 1000,
        maxReconnectAttempts: 3,
        requestTimeout: 120000, // 2 minutes
    };

    // State
    let eventSource = null;
    let isConnected = false;
    let reconnectAttempts = 0;
    let currentConversationId = null;
    let abortController = null;

    // Callbacks
    let callbacks = {
        onToken: null,
        onToolStarted: null,
        onToolCompleted: null,
        onError: null,
        onDone: null,
        onConnect: null,
        onDisconnect: null,
    };

    // ========================================
    // Initialization
    // ========================================
    function init(options = {}) {
        // Merge callbacks
        callbacks = { ...callbacks, ...options.callbacks };
        
        // Update config
        if (options.endpoint) CONFIG.endpoint = options.endpoint;
        if (options.conversationId) currentConversationId = options.conversationId;
    }

    // ========================================
    // Public API
    // ========================================
    function connect(conversationId = null) {
        if (conversationId) {
            currentConversationId = conversationId;
        }

        // If already connected, disconnect first
        if (eventSource) {
            disconnect();
        }

        return new Promise((resolve, reject) => {
            try {
                // Use fetch with ReadableStream for better control
                abortController = new AbortController();
                
                fetch(CONFIG.endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'text/event-stream',
                    },
                    body: JSON.stringify({
                        prompt: '', // Will be set when sending message
                        conversationId: currentConversationId || undefined,
                    }),
                    signal: abortController.signal,
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                    }
                    
                    if (!response.body) {
                        throw new Error('Response body is null');
                    }

                    isConnected = true;
                    reconnectAttempts = 0;
                    updateConnectionStatus(true);
                    callbacks.onConnect?.();

                    // Process the stream
                    return processStream(response.body);
                })
                .catch(error => {
                    if (error.name !== 'AbortError') {
                        handleError(error);
                        reject(error);
                    }
                });

                resolve();
            } catch (error) {
                handleError(error);
                reject(error);
            }
        });
    }

    function sendMessage(prompt, conversationId = null) {
        if (conversationId) {
            currentConversationId = conversationId;
        }

        // Disconnect existing connection
        disconnect();

        return new Promise((resolve, reject) => {
            try {
                abortController = new AbortController();
                
                fetch(CONFIG.endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'text/event-stream',
                    },
                    body: JSON.stringify({
                        prompt,
                        conversationId: currentConversationId || undefined,
                    }),
                    signal: abortController.signal,
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                    }
                    
                    if (!response.body) {
                        throw new Error('Response body is null');
                    }

                    isConnected = true;
                    reconnectAttempts = 0;
                    updateConnectionStatus(true);
                    callbacks.onConnect?.();

                    // Process the stream
                    return processStream(response.body);
                })
                .then(() => resolve())
                .catch(error => {
                    if (error.name !== 'AbortError') {
                        handleError(error);
                        reject(error);
                    }
                });
            } catch (error) {
                handleError(error);
                reject(error);
            }
        });
    }

    function disconnect() {
        if (abortController) {
            abortController.abort();
            abortController = null;
        }
        
        if (eventSource) {
            eventSource.close();
            eventSource = null;
        }

        if (isConnected) {
            isConnected = false;
            updateConnectionStatus(false);
            callbacks.onDisconnect?.();
        }
    }

    function setConversationId(id) {
        currentConversationId = id;
    }

    function getConversationId() {
        return currentConversationId;
    }

    function isStreaming() {
        return isConnected;
    }

    // ========================================
    // Stream Processing
    // ========================================
    async function processStream(readableStream) {
        const reader = readableStream.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        try {
            while (true) {
                const { done, value } = await reader.read();
                
                if (done) {
                    break;
                }

                buffer += decoder.decode(value, { stream: true });
                const events = parseSSE(buffer);
                
                // Keep incomplete event in buffer
                const lastNewline = buffer.lastIndexOf('\n\n');
                if (lastNewline !== -1) {
                    buffer = buffer.slice(lastNewline + 2);
                }
                
                for (const event of events) {
                    handleEvent(event);
                }
            }
        } catch (error) {
            if (error.name !== 'AbortError') {
                handleError(error);
            }
        } finally {
            reader.releaseLock();
            disconnect();
            callbacks.onDone?.();
        }
    }

    function parseSSE(buffer) {
        const events = [];
        const lines = buffer.split('\n');
        let event = { event: 'message', data: '', id: null, retry: null };

        for (const line of lines) {
            if (line === '') {
                // Empty line = event separator
                if (event.data) {
                    events.push({ ...event });
                    event = { event: 'message', data: '', id: null, retry: null };
                }
                continue;
            }

            if (line.startsWith(':')) {
                // Comment line, ignore
                continue;
            }

            const colonIndex = line.indexOf(':');
            if (colonIndex === -1) {
                // Malformed line, ignore
                continue;
            }

            const field = line.slice(0, colonIndex);
            const value = line.slice(colonIndex + 1).trimStart();

            switch (field) {
                case 'event':
                    event.event = value;
                    break;
                case 'data':
                    event.data += (event.data ? '\n' : '') + value;
                    break;
                case 'id':
                    event.id = value;
                    break;
                case 'retry':
                    event.retry = parseInt(value, 10);
                    break;
            }
        }

        return events;
    }

    function handleEvent(event) {
        switch (event.event) {
            case 'token':
                handleToken(event.data);
                break;
            case 'tool':
                handleToolEvent(event.data);
                break;
            case 'error':
                handleErrorEvent(event.data);
                break;
            case 'done':
                callbacks.onDone?.();
                break;
            default:
                // Unknown event type
                console.debug('Unknown SSE event:', event);
        }
    }

    function handleToken(data) {
        try {
            // Token data is JSON-encoded string
            const token = JSON.parse(data);
            callbacks.onToken?.(token);
        } catch {
            // If not JSON, treat as raw string
            callbacks.onToken?.(data);
        }
    }

    function handleToolEvent(data) {
        try {
            const toolEvent = JSON.parse(data);
            
            if (toolEvent.phase === 'STARTED') {
                callbacks.onToolStarted?.(toolEvent);
            } else if (toolEvent.phase === 'COMPLETED') {
                callbacks.onToolCompleted?.(toolEvent);
            }
        } catch (error) {
            console.error('Failed to parse tool event:', error);
        }
    }

    function handleErrorEvent(data) {
        callbacks.onError?.(data);
    }

    function handleError(error) {
        console.error('Streaming error:', error);
        callbacks.onError?.(error.message || 'Connection error');
        attemptReconnect();
    }

    // ========================================
    // Reconnection Logic
    // ========================================
    function attemptReconnect() {
        if (reconnectAttempts >= CONFIG.maxReconnectAttempts) {
            callbacks.onError?.('Max reconnection attempts reached');
            return;
        }

        reconnectAttempts++;
        const delay = CONFIG.reconnectDelay * Math.pow(2, reconnectAttempts - 1);
        
        setTimeout(() => {
            if (!isConnected) {
                connect().catch(() => {
                    // Reconnect will be attempted again in handleError
                });
            }
        }, delay);
    }

    // ========================================
    // UI Helpers
    // ========================================
    function updateConnectionStatus(connected) {
        const statusEl = document.getElementById('connectionStatus');
        if (statusEl) {
            if (connected) {
                statusEl.classList.remove('disconnected');
                statusEl.querySelector('.status-text').textContent = 'Connected';
            } else {
                statusEl.classList.add('disconnected');
                statusEl.querySelector('.status-text').textContent = 'Disconnected';
            }
        }
    }

    // ========================================
    // Public API Export
    // ========================================
    return {
        init,
        connect,
        sendMessage,
        disconnect,
        setConversationId,
        getConversationId,
        isStreaming,
    };
})();

// Export for global access
window.Streaming = Streaming;