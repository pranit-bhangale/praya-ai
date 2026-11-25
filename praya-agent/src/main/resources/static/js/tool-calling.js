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
    // Professional Dashboard Response Renderer
    // ========================================
    
    // Field label mapping for human-readable display
    const FIELD_LABELS = {
        // Common fields
        'id': 'ID',
        'name': 'Name',
        'title': 'Title',
        'description': 'Description',
        'summary': 'Summary',
        'status': 'Status',
        'type': 'Type',
        'category': 'Category',
        'created_at': 'Created',
        'updated_at': 'Updated',
        'created': 'Created',
        'updated': 'Updated',
        'date': 'Date',
        'timestamp': 'Timestamp',
        'url': 'URL',
        'link': 'Link',
        'website': 'Website',
        'email': 'Email',
        'phone': 'Phone',
        'address': 'Address',
        'location': 'Location',
        'city': 'City',
        'country': 'Country',
        'company': 'Company',
        'organization': 'Organization',
        'role': 'Role',
        'position': 'Position',
        'skill': 'Skill',
        'skills': 'Skills',
        'technology': 'Technology',
        'technologies': 'Technologies',
        'language': 'Language',
        'languages': 'Languages',
        'framework': 'Framework',
        'frameworks': 'Frameworks',
        'tool': 'Tool',
        'tools': 'Tools',
        'project': 'Project',
        'projects': 'Projects',
        'repository': 'Repository',
        'repositories': 'Repositories',
        'repo': 'Repository',
        'repos': 'Repositories',
        'github': 'GitHub',
        'linkedin': 'LinkedIn',
        'twitter': 'Twitter',
        'blog': 'Blog',
        'website': 'Website',
        'portfolio': 'Portfolio',
        'avatar': 'Avatar',
        'image': 'Image',
        'photo': 'Photo',
        'logo': 'Logo',
        'icon': 'Icon',
        'banner': 'Banner',
        'cover': 'Cover',
        'bio': 'Bio',
        'about': 'About',
        'headline': 'Headline',
        'tagline': 'Tagline',
        'experience': 'Experience',
        'education': 'Education',
        'certification': 'Certification',
        'certifications': 'Certifications',
        'qualification': 'Qualification',
        'qualifications': 'Qualifications',
        'degree': 'Degree',
        'school': 'School',
        'university': 'University',
        'institute': 'Institute',
        'year': 'Year',
        'duration': 'Duration',
        'start_date': 'Start Date',
        'end_date': 'End Date',
        'start': 'Start',
        'end': 'End',
        'current': 'Current',
        'is_current': 'Currently Active',
        'score': 'Score',
        'rating': 'Rating',
        'rank': 'Rank',
        'level': 'Level',
        'proficiency': 'Proficiency',
        'years': 'Years of Experience',
        'count': 'Count',
        'total': 'Total',
        'size': 'Size',
        'stars': 'Stars',
        'forks': 'Forks',
        'watchers': 'Watchers',
        'followers': 'Followers',
        'following': 'Following',
        'views': 'Views',
        'likes': 'Likes',
        'comments': 'Comments',
        'shares': 'Shares',
        'downloads': 'Downloads',
        'visits': 'Visits',
        'clicks': 'Clicks',
        'impressions': 'Impressions',
        'reach': 'Reach',
        'engagement': 'Engagement',
        'conversion': 'Conversion',
        'revenue': 'Revenue',
        'price': 'Price',
        'cost': 'Cost',
        'amount': 'Amount',
        'currency': 'Currency',
        'quantity': 'Quantity',
        'stock': 'Stock',
        'available': 'Available',
        'in_stock': 'In Stock',
        'active': 'Active',
        'enabled': 'Enabled',
        'disabled': 'Disabled',
        'visible': 'Visible',
        'hidden': 'Hidden',
        'public': 'Public',
        'private': 'Private',
        'featured': 'Featured',
        'pinned': 'Pinned',
        'archived': 'Archived',
        'deleted': 'Deleted',
        'draft': 'Draft',
        'published': 'Published',
        'pending': 'Pending',
        'approved': 'Approved',
        'rejected': 'Rejected',
        'cancelled': 'Cancelled',
        'completed': 'Completed',
        'in_progress': 'In Progress',
        'failed': 'Failed',
        'error': 'Error',
        'success': 'Success',
        'warning': 'Warning',
        'info': 'Info',
        'message': 'Message',
        'details': 'Details',
        'notes': 'Notes',
        'tags': 'Tags',
        'keywords': 'Keywords',
        'metadata': 'Metadata',
        'source': 'Source',
        'destination': 'Destination',
        'origin': 'Origin',
        'target': 'Target',
        'reference': 'Reference',
        'external_id': 'External ID',
        'internal_id': 'Internal ID',
        'uuid': 'UUID',
        'slug': 'Slug',
        'code': 'Code',
        'number': 'Number',
        'version': 'Version',
        'build': 'Build',
        'revision': 'Revision',
        'commit': 'Commit',
        'branch': 'Branch',
        'tag': 'Tag',
        'release': 'Release',
        'deployment': 'Deployment',
        'environment': 'Environment',
        'region': 'Region',
        'zone': 'Zone',
        'cluster': 'Cluster',
        'node': 'Node',
        'pod': 'Pod',
        'container': 'Container',
        'service': 'Service',
        'endpoint': 'Endpoint',
        'api': 'API',
        'method': 'Method',
        'path': 'Path',
        'query': 'Query',
        'params': 'Parameters',
        'headers': 'Headers',
        'body': 'Body',
        'response': 'Response',
        'request': 'Request',
        'latency': 'Latency',
        'throughput': 'Throughput',
        'bandwidth': 'Bandwidth',
        'cpu': 'CPU',
        'memory': 'Memory',
        'disk': 'Disk',
        'network': 'Network',
        'uptime': 'Uptime',
        'downtime': 'Downtime',
        'availability': 'Availability',
        'reliability': 'Reliability',
        'scalability': 'Scalability',
        'performance': 'Performance',
    };

    function formatFieldLabel(key) {
        // Check for exact match first
        if (FIELD_LABELS[key]) {
            return FIELD_LABELS[key];
        }
        
        // Check for partial matches (e.g., user_name -> Name)
        const lowerKey = key.toLowerCase();
        for (const [pattern, label] of Object.entries(FIELD_LABELS)) {
            if (lowerKey.includes(pattern) || lowerKey.endsWith('_' + pattern) || lowerKey.startsWith(pattern + '_')) {
                return label;
            }
        }
        
        // Convert snake_case to Title Case
        return key
            .replace(/_/g, ' ')
            .replace(/([A-Z])/g, ' $1')
            .replace(/^./, str => str.toUpperCase())
            .trim();
    }

    function formatFieldValue(key, value) {
        if (value === null || value === undefined) {
            return '<span class="data-empty">—</span>';
        }
        
        const lowerKey = key.toLowerCase();
        
        // URL detection
        if (typeof value === 'string' && (lowerKey.includes('url') || lowerKey.includes('link') || lowerKey.includes('website') || lowerKey.includes('github') || lowerKey.includes('linkedin') || lowerKey.includes('twitter'))) {
            if (value.startsWith('http')) {
                return `<a href="${escapeHtml(value)}" target="_blank" rel="noopener noreferrer" class="data-link">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                    ${escapeHtml(value.length > 50 ? value.substring(0, 50) + '…' : value)}
                </a>`;
            }
        }
        
        // Email detection
        if (typeof value === 'string' && lowerKey.includes('email') && value.includes('@')) {
            return `<a href="mailto:${escapeHtml(value)}" class="data-link">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                ${escapeHtml(value)}
            </a>`;
        }
        
        // Date detection
        if (typeof value === 'string' && (lowerKey.includes('date') || lowerKey.includes('created') || lowerKey.includes('updated') || lowerKey.includes('timestamp') || lowerKey.includes('time'))) {
            const date = new Date(value);
            if (!isNaN(date.getTime())) {
                return `<span class="data-date" title="${escapeHtml(value)}">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                    ${date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
                </span>`;
            }
        }
        
        // Boolean detection
        if (typeof value === 'boolean') {
            return value 
                ? '<span class="data-boolean data-boolean-true"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg> Yes</span>'
                : '<span class="data-boolean data-boolean-false"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg> No</span>';
        }
        
        // Number formatting
        if (typeof value === 'number') {
            if (Number.isInteger(value)) {
                if (lowerKey.includes('count') || lowerKey.includes('total') || lowerKey.includes('size') || lowerKey.includes('stars') || lowerKey.includes('forks') || lowerKey.includes('followers') || lowerKey.includes('views') || lowerKey.includes('likes')) {
                    return `<span class="data-number">${value.toLocaleString()}</span>`;
                }
                if (lowerKey.includes('price') || lowerKey.includes('cost') || lowerKey.includes('amount') || lowerKey.includes('revenue')) {
                    return `<span class="data-currency">$${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>`;
                }
                if (lowerKey.includes('percent') || lowerKey.includes('rate') || lowerKey.includes('ratio')) {
                    return `<span class="data-percent">${(value * 100).toFixed(1)}%</span>`;
                }
            }
            return `<span class="data-number">${value.toLocaleString()}</span>`;
        }
        
        // Array formatting
        if (Array.isArray(value)) {
            if (value.length === 0) {
                return '<span class="data-empty">None</span>';
            }
            if (value.every(v => typeof v === 'string')) {
                return value.map(v => `<span class="data-tag">${escapeHtml(v)}</span>`).join('');
            }
            return `<span class="data-array">${value.length} items</span>`;
        }
        
        // Object formatting (nested)
        if (typeof value === 'object') {
            return `<span class="data-object">{${Object.keys(value).length} fields}</span>`;
        }
        
        // Default string
        const strValue = String(value);
        if (strValue.length > 100) {
            return `<span class="data-text" title="${escapeHtml(strValue)}">${escapeHtml(strValue.substring(0, 100))}…</span>`;
        }
        return `<span class="data-text">${escapeHtml(strValue)}</span>`;
    }

    function getStatusInfo(responseData) {
        // Try to determine status from response
        if (!responseData) return { status: 'UNKNOWN', label: 'Unknown', class: 'status-unknown' };
        
        const data = typeof responseData === 'string' ? JSON.parse(responseData) : responseData;
        
        // Check for explicit status field
        if (data.status) {
            const status = String(data.status).toUpperCase();
            if (['SUCCESS', 'OK', 'COMPLETED', 'DONE', 'SUCCESSFUL'].includes(status)) {
                return { status: 'SUCCESS', label: 'Success', class: 'status-success' };
            }
            if (['FAILED', 'ERROR', 'FAILURE', 'ERR'].includes(status)) {
                return { status: 'ERROR', label: 'Failed', class: 'status-error' };
            }
            if (['PENDING', 'PROCESSING', 'IN_PROGRESS', 'LOADING'].includes(status)) {
                return { status: 'PENDING', label: 'Pending', class: 'status-pending' };
            }
            return { status: status, label: status, class: 'status-unknown' };
        }
        
        // Check for success/error indicators
        if (data.success === true) return { status: 'SUCCESS', label: 'Success', class: 'status-success' };
        if (data.success === false) return { status: 'ERROR', label: 'Failed', class: 'status-error' };
        if (data.error) return { status: 'ERROR', label: 'Error', class: 'status-error' };
        if (data.message && data.message.toLowerCase().includes('error')) return { status: 'ERROR', label: 'Error', class: 'status-error' };
        if (data.message && data.message.toLowerCase().includes('success')) return { status: 'SUCCESS', label: 'Success', class: 'status-success' };
        
        // Default to success if data exists
        return { status: 'SUCCESS', label: 'Success', class: 'status-success' };
    }

    function getSourceInfo(responseData) {
        if (!responseData) return 'Unknown';
        
        const data = typeof responseData === 'string' ? JSON.parse(responseData) : responseData;
        
        // Check for source field
        if (data.source) return String(data.source);
        if (data.provider) return String(data.provider);
        if (data.platform) return String(data.platform);
        if (data.api) return String(data.api);
        
        // Infer from data structure
        if (data.github || data.repositories || data.repos) return 'GitHub';
        if (data.linkedin || data.profile) return 'LinkedIn';
        if (data.twitter || data.tweets) return 'Twitter';
        if (data.blog || data.posts) return 'Blog';
        
        return 'API';
    }

    function getTotalRecords(responseData) {
        if (!responseData) return 0;
        
        const data = typeof responseData === 'string' ? JSON.parse(responseData) : responseData;
        
        // Check for explicit count fields
        if (typeof data.total === 'number') return data.total;
        if (typeof data.count === 'number') return data.count;
        if (typeof data.length === 'number') return data.length;
        if (typeof data.size === 'number') return data.size;
        if (typeof data.totalRecords === 'number') return data.totalRecords;
        if (typeof data.total_count === 'number') return data.total_count;
        
        // Check for data array
        if (Array.isArray(data.data)) return data.data.length;
        if (Array.isArray(data.items)) return data.items.length;
        if (Array.isArray(data.results)) return data.results.length;
        if (Array.isArray(data.records)) return data.records.length;
        if (Array.isArray(data)) return data.length;
        
        // Count objects in response
        if (data && typeof data === 'object') {
            const keys = Object.keys(data);
            if (keys.length === 1 && Array.isArray(data[keys[0]])) {
                return data[keys[0]].length;
            }
        }
        
        return 0;
    }

    function getDataArray(responseData) {
        if (!responseData) return [];
        
        const data = typeof responseData === 'string' ? JSON.parse(responseData) : responseData;
        
        // Common array locations
        if (Array.isArray(data.data)) return data.data;
        if (Array.isArray(data.items)) return data.items;
        if (Array.isArray(data.results)) return data.results;
        if (Array.isArray(data.records)) return data.records;
        if (Array.isArray(data)) return data;
        
        // Single object - wrap in array
        if (data && typeof data === 'object' && !Array.isArray(data)) {
            // Check if it's a wrapper object with one array property
            const keys = Object.keys(data);
            if (keys.length === 1 && Array.isArray(data[keys[0]])) {
                return data[keys[0]];
            }
            return [data];
        }
        
        return [];
    }

    function getMessage(responseData) {
        if (!responseData) return '';
        
        const data = typeof responseData === 'string' ? JSON.parse(responseData) : responseData;
        
        if (data.message) return String(data.message);
        if (data.msg) return String(data.msg);
        if (data.status_message) return String(data.status_message);
        
        return '';
    }

    function identifyKeyFields(item) {
        // Identify the most important fields for display
        const keys = Object.keys(item);
        const priorityFields = ['name', 'title', 'id', 'username', 'login', 'email', 'company', 'organization'];
        
        let primaryKey = keys.find(k => priorityFields.includes(k.toLowerCase()));
        if (!primaryKey) primaryKey = keys[0];
        
        return { primaryKey, keys };
    }

    function createDashboardView(responseData) {
        const statusInfo = getStatusInfo(responseData);
        const source = getSourceInfo(responseData);
        const totalRecords = getTotalRecords(responseData);
        const dataArray = getDataArray(responseData);
        const message = getMessage(responseData);
        
        let html = '';
        
        // Status Badge
        html += `
            <div class="dashboard-header">
                <div class="dashboard-status">
                    <span class="status-badge ${statusInfo.class}">${statusInfo.label}</span>
                    <span class="dashboard-source">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/><path d="M2 12h20"/></svg>
                        ${escapeHtml(source)}
                    </span>
                </div>
                <div class="dashboard-metric">
                    <span class="metric-value">${totalRecords.toLocaleString()}</span>
                    <span class="metric-label">Records</span>
                </div>
            </div>
        `;
        
        // Data Display
        if (dataArray.length > 0) {
            // Determine if we should use cards or table
            const sampleItem = dataArray[0];
            const keys = Object.keys(sampleItem);
            const hasComplexData = keys.some(k => Array.isArray(sampleItem[k]) || (typeof sampleItem[k] === 'object' && sampleItem[k] !== null));
            const useTable = keys.length > 6 || dataArray.length > 10;
            
            if (useTable) {
                html += createTableView(dataArray);
            } else {
                html += createCardView(dataArray);
            }
        } else {
            html += `
                <div class="dashboard-empty">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="9" y1="9" x2="15" y2="15"/><line x1="15" y1="9" x2="9" y2="15"/></svg>
                    <p>No data available</p>
                </div>
            `;
        }
        
        // Message
        if (message) {
            html += `
                <div class="dashboard-message">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                    <span>${escapeHtml(message)}</span>
                </div>
            `;
        }
        
        return html;
    }

    function createCardView(dataArray) {
        let html = '<div class="dashboard-cards">';
        
        dataArray.forEach((item, index) => {
            const keys = Object.keys(item);
            const { primaryKey } = identifyKeyFields(item);
            const primaryValue = item[primaryKey] || `Item ${index + 1}`;
            
            html += `
                <div class="dashboard-card" data-index="${index}">
                    <div class="card-header">
                        <div class="card-primary">
                            <span class="card-title">${escapeHtml(String(primaryValue))}</span>
                        </div>
                        <div class="card-index">${index + 1}</div>
                    </div>
                    <div class="card-fields">
            `;
            
            keys.forEach(key => {
                if (key === primaryKey) return; // Skip primary key as it's in header
                const value = item[key];
                if (value === null || value === undefined || (typeof value === 'string' && value === '')) return;
                
                html += `
                    <div class="card-field">
                        <span class="field-label">${formatFieldLabel(key)}</span>
                        <span class="field-value">${formatFieldValue(key, value)}</span>
                    </div>
                `;
            });
            
            html += `
                    </div>
                </div>
            `;
        });
        
        html += '</div>';
        return html;
    }

    function createTableView(dataArray) {
        // Collect all unique keys
        const allKeys = new Set();
        dataArray.forEach(item => {
            Object.keys(item).forEach(k => allKeys.add(k));
        });
        const keys = Array.from(allKeys);
        
        // Identify primary key for first column
        const { primaryKey } = identifyKeyFields(dataArray[0]);
        const sortedKeys = [primaryKey, ...keys.filter(k => k !== primaryKey)];
        
        let html = `
            <div class="dashboard-table-wrapper">
                <table class="dashboard-table">
                    <thead>
                        <tr>
        `;
        
        sortedKeys.forEach(key => {
            html += `<th>${formatFieldLabel(key)}</th>`;
        });
        
        html += `
                        </tr>
                    </thead>
                    <tbody>
        `;
        
        dataArray.forEach((item, rowIndex) => {
            html += `<tr data-index="${rowIndex}">`;
            
            sortedKeys.forEach(key => {
                const value = item[key];
                html += `<td>${formatFieldValue(key, value)}</td>`;
            });
            
            html += `</tr>`;
        });
        
        html += `
                    </tbody>
                </table>
            </div>
        `;
        
        return html;
    }

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
                    <div class="dashboard-view">${createDashboardView(toolCallEvent.responseData)}</div>
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
            'profile': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
            'contact': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>',
            'about': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>',
            'skill': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>',
            'cert': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="7"/><polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"/></svg>',
            'qualification': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>',
            'blog': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>',
            'project': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>',
            'repo': '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>',
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