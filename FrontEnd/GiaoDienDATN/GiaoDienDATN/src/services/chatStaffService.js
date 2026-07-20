// services/chatStaffService.js
import api from './api';

const chatStaffService = {
  // FIX: StaffChat.vue gọi hàm này nhưng bản cũ chưa có -> throw lỗi mỗi lần load dashboard.
  // Trả về TẤT CẢ chat đang ACTIVE (mọi staff), dùng cho màn hình danh sách hội thoại
  // kiểu admin/dashboard. BE cần thêm endpoint GET /api/chat/staff/active tương ứng.
  getAllActiveChats: async () => {
    try {
      const response = await api.get('/chat/staff/active');
      return response || [];
    } catch (error) {
      console.error('getAllActiveChats error:', error);
      throw error;
    }
  },
  

  getWaitingCount: async () => {
    try {
      const response = await api.get('/chat/staff/queue/count');
      return response?.waitingCount ?? 0;
    } catch (error) {
      console.error('getWaitingCount error:', error);
      return 0;
    }
  },

  getChatsByStaff: async (staffUserId) => {
    try {
      if (!staffUserId) return [];
      const response = await api.get(`/chat/staff/${staffUserId}/chats`);
      return response || [];
    } catch (error) {
      console.error('getChatsByStaff error:', error);
      throw error;
    }
  },

  getMessages: async (chatId) => {
    try {
      if (!chatId) return [];
      const response = await api.get(`/chat/staff/${chatId}/messages`);
      return response || [];
    } catch (error) {
      console.error('getMessages error:', error);
      throw error;
    }
  },

  sendMessage: async (chatId, userId, content) => {
    try {
      if (!chatId || !userId || !content || !content.trim()) throw new Error('Missing required fields');
      const payload = {
        chatId: Number(chatId),
        userId: Number(userId),
        message: content.trim(),
        isAi: false
      };
      return await api.post('/chat/staff/messages', payload);
    } catch (error) {
      console.error('sendMessage error:', error);
      throw error;
    }
  },

  markAsRead: async (chatId, userId) => {
    try {
      if (!chatId || !userId) return;
      return await api.put(`/chat/staff/${chatId}/read/${userId}`);
    } catch (error) {
      console.error('markAsRead error:', error);
      throw error;
    }
  },

  acceptChat: async (chatId, staffUserId) => {
    try {
      return await api.post(`/chat/staff/${chatId}/accept/${staffUserId}`);
    } catch (error) {
      console.error('acceptChat error:', error);
      throw error;
    }
  },

  closeChat: async (chatId, staffUserId) => {
    try {
      return await api.post(`/chat/staff/${chatId}/close/${staffUserId}`);
    } catch (error) {
      console.error('closeChat error:', error);
      throw error;
    }
  },
  
  reopenChat: async (chatId, staffUserId) => {
    try {
      return await api.post(`/chat/staff/${chatId}/reopen/${staffUserId}`);
    } catch (error) {
      console.error('reopenChat error:', error);
      throw error;
    }
  }
};

export default chatStaffService;