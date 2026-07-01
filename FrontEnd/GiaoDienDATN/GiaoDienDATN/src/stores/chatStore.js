// stores/chatStore.js
import { defineStore } from 'pinia';
import chatService from '../services/chatService';
import { useWebSocket } from '../composables/useWebSocket';

export const useChatStore = defineStore('chat', {
  state: () => ({
    conversations: [],
    selectedConversation: null,
    messages: [],
    unreadCount: 0,
    loading: {
      conversations: false,
      messages: false,
      sending: false,
    },
    searchKeyword: '',
    error: null,
    ws: null,
    isWebSocketConnected: false,
    wsListeners: [],
    currentCustomerId: null,
    processedMessageIds: new Set(),
  }),

  getters: {
    filteredConversations: (state) => {
      const keyword = state.searchKeyword.toLowerCase().trim();
      if (!keyword) return state.conversations;

      return state.conversations.filter(item =>
        item.customerName?.toLowerCase().includes(keyword) ||
        item.phone?.includes(keyword)
      );
    },

    activeConversations: (state) => {
      return state.conversations.filter(c => c.status === 'ACTIVE');
    },

    closedConversations: (state) => {
      return state.conversations.filter(c => c.status === 'CLOSED');
    },
  },

  actions: {
    // ===== WEBSOCKET =====
    initWebSocket() {
      if (this.ws) {
        console.log('WebSocket already initialized');
        return this.ws;
      }

      try {
        const { connect, onMessage, startPing } = useWebSocket();
        this.ws = connect('/ws/chat');

        if (this.ws) {
          const newMessageListener = onMessage('new_message', this.handleNewMessage);
          const connectionListener = onMessage('connection', this.handleConnection);

          this.wsListeners = [newMessageListener, connectionListener];
          this.pingInterval = startPing();
          this.isWebSocketConnected = true;
          console.log('✅ WebSocket initialized successfully');
        }

        return this.ws;
      } catch (error) {
        console.error('Failed to initialize WebSocket:', error);
        this.isWebSocketConnected = false;
        return null;
      }
    },

    disconnectWebSocket() {
      if (this.ws) {
        try {
          this.wsListeners.forEach(cleanup => {
            if (typeof cleanup === 'function') cleanup();
          });
          this.wsListeners = [];

          if (this.pingInterval) {
            clearInterval(this.pingInterval);
            this.pingInterval = null;
          }

          this.ws.close();
          this.ws = null;
          this.isWebSocketConnected = false;
          console.log('WebSocket disconnected');
        } catch (error) {
          console.error('Error disconnecting WebSocket:', error);
        }
      }
    },

    // ===== WEBSOCKET HANDLERS =====
    handleNewMessage(data) {
      console.log('📨 New message received:', data);
      const messageData = data.data || data;
      const chatId = messageData.chatId;
      const message = messageData.message;

      if (!chatId || !message) return;

      // Kiểm tra tin nhắn đã xử lý chưa
      if (this.processedMessageIds.has(message.messageId)) {
        console.log('⏭️ Duplicate message skipped:', message.messageId);
        return;
      }

      this.processedMessageIds.add(message.messageId);
      
      if (this.processedMessageIds.size > 10000) {
        const ids = Array.from(this.processedMessageIds);
        const toRemove = ids.slice(0, 5000);
        toRemove.forEach(id => this.processedMessageIds.delete(id));
      }

      // Cập nhật conversation
      const conv = this.conversations.find(c => c.chatId === chatId);
      if (conv) {
        conv.lastMessage = message.messageContent;
        conv.lastMessageTime = message.sentDate || new Date().toISOString();

        // Nếu tin nhắn từ staff và không phải conversation đang xem
        if (message.userRole === 'STAFF' && this.selectedConversation?.chatId !== chatId) {
          conv.unreadCount = (conv.unreadCount || 0) + 1;
          this.unreadCount++;
        }
        
        // Nếu tin nhắn từ customer và staff đang xem conversation khác
        if (message.userRole === 'CUSTOMER' && this.selectedConversation?.chatId !== chatId) {
          conv.unreadCount = (conv.unreadCount || 0) + 1;
          this.unreadCount++;
        }
      }

      // Thêm tin nhắn vào danh sách
      if (this.selectedConversation?.chatId === chatId) {
        const exists = this.messages.some(m => m.messageId === message.messageId);
        if (!exists) {
          this.messages.push({
            ...message,
            isFromUser: message.userRole === 'CUSTOMER'
          });
          
          // Đánh dấu đã đọc nếu đang xem
          if (this.currentCustomerId) {
            this.markAsRead(chatId, this.currentCustomerId);
          }
        }
      }
    },

    handleConnection(data) {
      console.log('🔗 Connection event:', data);
      this.isWebSocketConnected = true;
    },

    // ===== CONVERSATIONS =====
    async fetchConversations(customerId) {
      if (!customerId) {
        console.error('fetchConversations: customerId is required');
        this.loading.conversations = false;
        return [];
      }

      this.currentCustomerId = customerId;
      this.loading.conversations = true;
      this.error = null;

      try {
        const response = await chatService.getConversations(customerId);
        this.conversations = response || [];
        await this.updateUnreadCount(customerId);
        return this.conversations;
      } catch (error) {
        this.error = error.message || 'Không thể tải danh sách cuộc trò chuyện';
        console.error('fetchConversations error:', error);
        throw error;
      } finally {
        this.loading.conversations = false;
      }
    },

    // ===== MESSAGES =====
    async fetchMessages(conversationId) {
      if (!conversationId) return;

      this.loading.messages = true;
      this.error = null;

      try {
        const response = await chatService.getMessages(conversationId);
        this.messages = response || [];

        const conv = this.conversations.find(c => c.chatId === conversationId);
        if (conv && conv.status === 'ACTIVE' && this.currentCustomerId) {
          conv.unreadCount = 0;
          await this.markAsRead(conversationId, this.currentCustomerId);
        }

        return this.messages;
      } catch (error) {
        this.error = error.message || 'Không thể tải tin nhắn';
        console.error('fetchMessages error:', error);
        throw error;
      } finally {
        this.loading.messages = false;
      }
    },

    async selectConversation(conversation) {
      if (!conversation) return;

      if (this.selectedConversation?.chatId !== conversation.chatId) {
        this.selectedConversation = conversation;
        await this.fetchMessages(conversation.chatId);
      }
    },

    // ===== SEND MESSAGE =====
    async sendMessage(content) {
      const userId = this.currentCustomerId;

      if (!this.selectedConversation) {
        throw new Error('Không có cuộc trò chuyện nào được chọn');
      }
      if (!content?.trim()) {
        throw new Error('Tin nhắn không được để trống');
      }

      if (!userId) {
        const user = JSON.parse(localStorage.getItem('userInfo') || '{}');
        const fallbackId = user?.customerId || user?.userId || user?.id;
        if (fallbackId) {
          this.currentCustomerId = fallbackId;
        } else {
          throw new Error('Không xác định được người dùng. Vui lòng đăng nhập lại!');
        }
      }

      this.loading.sending = true;
      this.error = null;

      try {
        const response = await chatService.sendMessage(
          this.selectedConversation.chatId,
          userId,
          content.trim()
        );

        const conv = this.conversations.find(c => c.chatId === this.selectedConversation.chatId);
        if (conv) {
          conv.lastMessage = content.trim();
          conv.lastMessageTime = new Date().toISOString();
          conv.unreadCount = 0;
        }

        await this.updateUnreadCount(userId);

        return response;
      } catch (error) {
        this.error = error.message || 'Không thể gửi tin nhắn';
        console.error('sendMessage error:', error);
        throw error;
      } finally {
        this.loading.sending = false;
      }
    },

    // ===== READ STATUS =====
    async markAsRead(conversationId, userId) {
      if (!conversationId || !userId) return;

      try {
        await chatService.markAsRead(conversationId, userId);
        const conv = this.conversations.find(c => c.chatId === conversationId);
        if (conv) {
          conv.unreadCount = 0;
        }
        await this.updateUnreadCount(userId);
      } catch (error) {
        console.error('markAsRead error:', error);
      }
    },

    // ===== CONVERSATION MANAGEMENT =====
    async createConversation(customerId) {
      try {
        const response = await chatService.createConversation(customerId);
        if (response) {
          this.conversations.unshift(response);
          this.selectedConversation = response;
          this.currentCustomerId = customerId;
          return response;
        }
        return null;
      } catch (error) {
        console.error('createConversation error:', error);
        throw error;
      }
    },

    async closeConversation(conversationId) {
      try {
        await chatService.closeConversation(conversationId);
        const conv = this.conversations.find(c => c.chatId === conversationId);
        if (conv) {
          conv.status = 'CLOSED';
          conv.unreadCount = 0;
        }
        await this.updateUnreadCount(this.currentCustomerId);
        return true;
      } catch (error) {
        console.error('closeConversation error:', error);
        throw error;
      }
    },

    // ===== UNREAD COUNT =====
    async updateUnreadCount(customerId) {
      if (!customerId) return 0;

      try {
        const response = await chatService.getUnreadCount(customerId);
        this.unreadCount = response?.unreadCount || 0;
        return this.unreadCount;
      } catch (error) {
        console.error('updateUnreadCount error:', error);
        return 0;
      }
    },

    // ===== SEARCH =====
    setSearchKeyword(keyword) {
      this.searchKeyword = keyword;
    },

    // ===== RESET =====
    resetState() {
      this.conversations = [];
      this.selectedConversation = null;
      this.messages = [];
      this.unreadCount = 0;
      this.loading = {
        conversations: false,
        messages: false,
        sending: false,
      };
      this.searchKeyword = '';
      this.error = null;
      this.isWebSocketConnected = false;
      this.currentCustomerId = null;
      this.processedMessageIds = new Set();
      this.disconnectWebSocket();
    },
  },
});