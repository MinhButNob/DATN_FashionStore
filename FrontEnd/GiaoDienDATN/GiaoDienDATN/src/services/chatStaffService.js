// services/chatStaffService.js
import api from './api';

const chatStaffService = {
  getAllActiveChats: async () => {
    try {
      const response = await api.get('/chat-nhan-vien/active');
      return response || [];
    } catch (error) {
      console.error('getAllActiveChats error:', error);
      throw error;
    }
  },

  getChatsByAdmin: async (adminId) => {
    try {
      if (!adminId) return [];
      const response = await api.get(`/chat-nhan-vien/admin/${adminId}`);
      return response || [];
    } catch (error) {
      console.error('getChatsByAdmin error:', error);
      throw error;
    }
  },

  getMessages: async (chatId) => {
    try {
      if (!chatId) return [];
      const response = await api.get(`/chat-nhan-vien/${chatId}/messages`);
      return response || [];
    } catch (error) {
      console.error('getMessages error:', error);
      throw error;
    }
  },

  sendMessage: async (chatId, userId, content) => {
    try {
      console.log('📤 Staff sendMessage called:', { chatId, userId, content });
      
      if (!chatId) throw new Error('chatId is required');
      if (!userId) throw new Error('userId is required');
      if (!content || !content.trim()) throw new Error('content is required');

      const payload = {
        chatId: Number(chatId),
        userId: Number(userId),
        message: content.trim(),
        isAi: false
      };
      
      console.log('📤 Staff sendMessage payload:', payload);
      
      const response = await api.post('/chat-nhan-vien/messages', payload);
      console.log('✅ Staff sendMessage response:', response);
      return response;
    } catch (error) {
      console.error('sendMessage error:', error);
      if (error.response) {
        console.error('Response data:', error.response.data);
      }
      throw error;
    }
  },

  markAsRead: async (chatId, userId) => {
    try {
      if (!chatId || !userId) return;
      const response = await api.put(`/chat-nhan-vien/${chatId}/read/${userId}`);
      return response;
    } catch (error) {
      console.error('markAsRead error:', error);
      throw error;
    }
  },

  assignAdmin: async (chatId, adminId) => {
    try {
      const response = await api.put(`/chat-nhan-vien/${chatId}/assign/${adminId}`);
      return response;
    } catch (error) {
      console.error('assignAdmin error:', error);
      throw error;
    }
  },

  closeChat: async (chatId) => {
    try {
      const response = await api.put(`/chat-nhan-vien/${chatId}/close`);
      return response;
    } catch (error) {
      console.error('closeChat error:', error);
      throw error;
    }
  },

  countUnreadMessages: async (chatId, userId) => {
    try {
      const response = await api.get(`/chat-nhan-vien/${chatId}/unread/${userId}`);
      return response || 0;
    } catch (error) {
      console.error('countUnreadMessages error:', error);
      return 0;
    }
  },
};

export default chatStaffService;