// services/chatService.js
import api from './api';

/**
 * Chat Service cho khách hàng (Customer)
 * Tương ứng với ChatCustomerController trong Backend
 */
const chatService = {
  /**
   * Lấy danh sách conversations của customer
   * GET /api/chat/customer/conversations?customerId={customerId}
   */
  getConversations: async (customerId) => {
    try {
      if (!customerId) {
        console.warn('getConversations: customerId is required');
        return [];
      }
      const response = await api.get('/chat/customer/conversations', {
        params: { customerId: Number(customerId) }
      });
      return response || [];
    } catch (error) {
      console.error('getConversations error:', error);
      throw error;
    }
  },

  /**
   * Lấy tin nhắn của chat
   * GET /api/chat/customer/{chatId}/messages
   */
  getMessages: async (chatId) => {
    try {
      if (!chatId) {
        console.warn('getMessages: chatId is required');
        return [];
      }
      const response = await api.get(`/chat/customer/${chatId}/messages`);
      return response || [];
    } catch (error) {
      console.error('getMessages error:', error);
      throw error;
    }
  },

  /**
   * Gửi tin nhắn từ customer
   * POST /api/chat/customer/{chatId}/messages
   * Body: { userId, message, isAi }
   */
  sendMessage: async (chatId, userId, content) => {
    try {
      if (!chatId) throw new Error('chatId is required');
      if (!userId) throw new Error('userId is required');
      if (!content || !content.trim()) throw new Error('content is required');

      const response = await api.post(`/chat/customer/${chatId}/messages`, {
        chatId: Number(chatId),
        userId: Number(userId),
        message: content.trim(),
        isAi: false
      });
      return response;
    } catch (error) {
      console.error('sendMessage error:', error);
      throw error;
    }
  },

  /**
   * Đánh dấu đã đọc
   * PUT /api/chat/customer/{chatId}/read?userId={userId}
   */
  markAsRead: async (chatId, userId) => {
    try {
      if (!chatId || !userId) return;
      const response = await api.put(`/chat/customer/${chatId}/read`, null, {
        params: { userId: Number(userId) }
      });
      return response;
    } catch (error) {
      console.error('markAsRead error:', error);
      throw error;
    }
  },

  /**
   * Lấy số tin nhắn chưa đọc
   * GET /api/chat/customer/unread-count?customerId={customerId}
   */
  getUnreadCount: async (customerId) => {
    try {
      if (!customerId) return { unreadCount: 0 };
      const response = await api.get('/chat/customer/unread-count', {
        params: { customerId: Number(customerId) }
      });
      return response || { unreadCount: 0 };
    } catch (error) {
      console.error('getUnreadCount error:', error);
      return { unreadCount: 0 };
    }
  },

  /**
   * Yêu cầu chat với nhân viên (tạo mới hoặc lấy chat WAITING/ACTIVE)
   * POST /api/chat/customer/create
   * Body: { customerId, message, isAi }
   */
  requestStaffChat: async (customerId, firstMessage = null) => {
    try {
      if (!customerId) throw new Error('customerId is required');

      const response = await api.post('/chat/customer/create', {
        customerId: Number(customerId),
        message: firstMessage || null,
        isAi: false
      });
      return response;
    } catch (error) {
      console.error('createConversation error:', error);
      throw error;
    }
  },

  /**
   * Đóng conversation
   * PUT /api/chat/customer/{chatId}/close
   */
  closeConversation: async (chatId) => {
    try {
      if (!chatId) throw new Error('chatId is required');
      const response = await api.put(`/chat/customer/${chatId}/close`);
      return response;
    } catch (error) {
      console.error('closeConversation error:', error);
      throw error;
    }
  } };

export default chatService;