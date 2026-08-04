// stores/chatStore.js
import { defineStore } from "pinia";
import chatService from "../services/chatService";
import chatStaffService from "../services/chatStaffService";
import { useWebSocket } from "../composables/useWebSocket";

// =====================================================================
// SINGLETON WEBSOCKET — Tạo NGOÀI defineStore để tránh gắn ref() vào
// component effect scope.
// =====================================================================
const _wsManager = useWebSocket();

export const useChatStore = defineStore("chat", {
  state: () => ({
    conversations: [],
    selectedConversation: null,
    messages: [],
    unreadCount: 0,
    waitingCount: 0,
    loading: {
      conversations: false,
      messages: false,
      sending: false,
    },
    searchKeyword: "",
    error: null,
    ws: null,
    isWebSocketConnected: false,
    wsListeners: [],
    currentCustomerId: null,
    processedMessageIds: new Set(),
    availableChatTypes: [],
    selectedChatType: null,
    availableStaff: [],
  }),

  getters: {
    filteredConversations: (state) => {
      const keyword = (state.searchKeyword || "").toLowerCase().trim();
      if (!keyword) return state.conversations || [];
      return (state.conversations || []).filter(
        (item) =>
          item?.customerName?.toLowerCase().includes(keyword) ||
          item?.phone?.includes(keyword),
      );
    },
    activeConversations: (state) => {
      return (state.conversations || []).filter((c) => c.status === "ACTIVE");
    },
    closedConversations: (state) => {
      return (state.conversations || []).filter((c) => c.status === "CLOSED");
    },
    waitingConversations: (state) => {
      return (state.conversations || []).filter((c) => c.status === "WAITING");
    },
  },

  actions: {
    // ===== WEBSOCKET =====
    initWebSocket(staffUserId) {
      console.log(
        "[chatStore] initWebSocket called, staffUserId:",
        staffUserId,
      );

      if (this.ws) {
        console.log(
          "[chatStore] WebSocket already initialized, readyState:",
          this.ws.readyState,
        );
        return this.ws;
      }

      try {
        // ✅ Kết nối WebSocket
        this.ws = _wsManager.connect("/ws/chat");
        console.log("[chatStore] WebSocket connect result:", this.ws);

        if (this.ws) {
          window.chatWs = this.ws;

          // ✅ Đợi socket mở rồi mới identify
          if (this.ws.readyState === WebSocket.OPEN) {
            console.log("[chatStore] Socket already open, identifying...");
            if (staffUserId) {
              _wsManager.identify("STAFF", staffUserId);
            }
          } else {
            // Nếu chưa mở, đợi sự kiện open
            const onOpen = () => {
              console.log("[chatStore] Socket opened, identifying...");
              if (staffUserId) {
                _wsManager.identify("STAFF", staffUserId);
              }
              this.ws.removeEventListener("open", onOpen);
            };
            this.ws.addEventListener("open", onOpen);
          }

          this.wsListeners = [
            _wsManager.onMessage("new_message", this.handleNewMessage),
            _wsManager.onMessage("chat_assigned", this.handleChatStatusEvent),
            _wsManager.onMessage("chat_accepted", this.handleChatStatusEvent),
            _wsManager.onMessage("chat_closed", this.handleChatStatusEvent),
            _wsManager.onMessage("chat_reopened", this.handleChatStatusEvent),
            _wsManager.onMessage(
              "chat_transferred",
              this.handleChatTransferred,
            ),
            _wsManager.onMessage("queue_updated", this.handleQueueUpdated),
            _wsManager.onMessage("registered", this.handleConnection),
          ];

          this.isWebSocketConnected = true;
          console.log("[chatStore] WebSocket initialized successfully");
        } else {
          console.error("[chatStore] Failed to create WebSocket instance");
        }

        return this.ws;
      } catch (error) {
        console.error("[chatStore] Failed to initialize WebSocket:", error);
        this.isWebSocketConnected = false;
        return null;
      }
    },

    disconnectWebSocket() {
      console.log("[chatStore] disconnectWebSocket called");
      if (this.ws) {
        try {
          (this.wsListeners || []).forEach((cleanup) => {
            if (typeof cleanup === "function") cleanup();
          });
          this.wsListeners = [];
          _wsManager.disconnect();
          this.ws = null;
          this.isWebSocketConnected = false;
          window.chatWs = null;
          console.log("[chatStore] WebSocket disconnected");
        } catch (error) {
          console.error("[chatStore] Error disconnecting WebSocket:", error);
        }
      }
    },

    // ===== WEBSOCKET HANDLERS =====
    handleNewMessage(data) {
      console.log("[chatStore] New message received:", data);
      const messageData = data.data || data;
      const chatId = messageData.chatId;
      const message = messageData.message;
      if (!chatId || !message) return;

      if (this.processedMessageIds.has(message.messageId)) {
        return;
      }
      this.processedMessageIds.add(message.messageId);

      if (this.processedMessageIds.size > 10000) {
        const ids = Array.from(this.processedMessageIds);
        ids.slice(0, 5000).forEach((id) => this.processedMessageIds.delete(id));
      }

      const conv = (this.conversations || []).find((c) => c.chatId === chatId);
      if (conv) {
        conv.lastMessage = message.messageContent;
        conv.lastMessageTime = message.sentDate || new Date().toISOString();

        if (this.selectedConversation?.chatId !== chatId) {
          conv.unreadCount = (conv.unreadCount || 0) + 1;
          this.unreadCount++;
        }
      }

      if (this.selectedConversation?.chatId === chatId) {
        const exists = (this.messages || []).some(
          (m) => m.messageId === message.messageId,
        );
        if (!exists) {
          this.messages.push({
            ...message,
            isFromUser: message.userRole === "CUSTOMER",
          });

          if (this.currentCustomerId) {
            this.markAsRead(chatId, this.currentCustomerId);
          }
        }
      }
    },

    handleConnection(data) {
      console.log("[chatStore] Connection event:", data);
      this.isWebSocketConnected = true;
    },

    handleChatStatusEvent(data) {
      const chat = data.data || data;
      if (!chat?.chatId) return;
      console.log("[chatStore] Chat status event:", chat.status, chat.chatId);

      const idx = (this.conversations || []).findIndex(
        (c) => c.chatId === chat.chatId,
      );
      if (idx >= 0) {
        this.conversations[idx] = { ...this.conversations[idx], ...chat };
      } else {
        this.conversations.unshift(chat);
      }
      if (this.selectedConversation?.chatId === chat.chatId) {
        this.selectedConversation = { ...this.selectedConversation, ...chat };
      }
    },

    handleChatTransferred(data) {
      const chat = data.data || data;
      if (!chat?.chatId) return;
      console.log("[chatStore] Chat transferred:", chat);

      const idx = (this.conversations || []).findIndex(
        (c) => c.chatId === chat.chatId,
      );
      if (idx >= 0) {
        this.conversations[idx] = { ...this.conversations[idx], ...chat };
      } else {
        this.conversations.unshift(chat);
      }
      if (this.selectedConversation?.chatId === chat.chatId) {
        this.selectedConversation = { ...this.selectedConversation, ...chat };
      }
    },

    handleQueueUpdated(data) {
      const payload = data.data || data;
      this.waitingCount = payload.waitingCount ?? this.waitingCount;
    },

    // ===== CHAT TYPES =====
    async fetchChatTypes() {
      try {
        const types = await chatService.getChatTypes();
        this.availableChatTypes = types || [];
        return this.availableChatTypes;
      } catch (error) {
        console.error("[chatStore] fetchChatTypes error:", error);
        return [];
      }
    },

    setSelectedChatType(type) {
      this.selectedChatType = type;
    },

    // ===== CONVERSATIONS =====
    async fetchConversations(customerId) {
      if (!customerId) {
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
        this.error = error.message || "Không thể tải danh sách cuộc trò chuyện";
        console.error("[chatStore] fetchConversations error:", error);
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
        const conv = (this.conversations || []).find(
          (c) => c.chatId === conversationId,
        );
        if (conv && conv.status === "ACTIVE" && this.currentCustomerId) {
          conv.unreadCount = 0;
          await this.markAsRead(conversationId, this.currentCustomerId);
        }
        return this.messages;
      } catch (error) {
        this.error = error.message || "Không thể tải tin nhắn";
        console.error("[chatStore] fetchMessages error:", error);
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
        throw new Error("Không có cuộc trò chuyện nào được chọn");
      }
      if (!content?.trim()) {
        throw new Error("Tin nhắn không được để trống");
      }
      if (!userId) {
        const user = JSON.parse(localStorage.getItem("userInfo") || "{}");
        const fallbackId = user?.customerId || user?.userId || user?.id;
        if (fallbackId) {
          this.currentCustomerId = fallbackId;
        } else {
          throw new Error(
            "Không xác định được người dùng. Vui lòng đăng nhập lại!",
          );
        }
      }
      this.loading.sending = true;
      this.error = null;
      try {
        const response = await chatService.sendMessage(
          this.selectedConversation.chatId,
          userId,
          content.trim(),
        );
        const conv = (this.conversations || []).find(
          (c) => c.chatId === this.selectedConversation.chatId,
        );
        if (conv) {
          conv.lastMessage = content.trim();
          conv.lastMessageTime = new Date().toISOString();
          conv.unreadCount = 0;
        }
        await this.updateUnreadCount(userId);
        return response;
      } catch (error) {
        this.error = error.message || "Không thể gửi tin nhắn";
        console.error("[chatStore] sendMessage error:", error);
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
        const conv = (this.conversations || []).find(
          (c) => c.chatId === conversationId,
        );
        if (conv) {
          conv.unreadCount = 0;
        }
        await this.updateUnreadCount(userId);
      } catch (error) {
        console.error("[chatStore] markAsRead error:", error);
      }
    },

    // ===== CONVERSATION MANAGEMENT =====
    async createConversation(customerId, chatType = null) {
      try {
        const response = await chatService.requestStaffChat(
          customerId,
          null,
          chatType,
        );
        if (response) {
          this.conversations.unshift(response);
          this.selectedConversation = response;
          this.currentCustomerId = customerId;
          return response;
        }
        return null;
      } catch (error) {
        console.error("[chatStore] createConversation error:", error);
        throw error;
      }
    },

    async closeConversation(conversationId) {
      try {
        await chatService.closeConversation(conversationId);
        const conv = (this.conversations || []).find(
          (c) => c.chatId === conversationId,
        );
        if (conv) {
          conv.status = "CLOSED";
          conv.unreadCount = 0;
        }
        await this.updateUnreadCount(this.currentCustomerId);
        return true;
      } catch (error) {
        console.error("[chatStore] closeConversation error:", error);
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
        console.error("[chatStore] updateUnreadCount error:", error);
        return 0;
      }
    },

    // ===== SEARCH =====
    setSearchKeyword(keyword) {
      this.searchKeyword = keyword || "";
    },

    // ===== RESET =====
    resetState() {
      this.conversations = [];
      this.selectedConversation = null;
      this.messages = [];
      this.unreadCount = 0;
      this.waitingCount = 0;
      this.loading = {
        conversations: false,
        messages: false,
        sending: false,
      };
      this.searchKeyword = "";
      this.error = null;
      this.isWebSocketConnected = false;
      this.currentCustomerId = null;
      this.processedMessageIds = new Set();
      this.availableChatTypes = [];
      this.selectedChatType = null;
      this.availableStaff = [];
      this.disconnectWebSocket();
    },
  },
});
