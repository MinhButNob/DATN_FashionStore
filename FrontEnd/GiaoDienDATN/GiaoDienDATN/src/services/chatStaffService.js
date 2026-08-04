// services/chatStaffService.js
import api from "./api";

const chatStaffService = {
  /**
   * Lấy danh sách loại chat
   * GET /api/chat/staff/types
   */
  getChatTypes: async () => {
    try {
      const response = await api.get("/chat/staff/types");
      return response || [];
    } catch (error) {
      console.error("getChatTypes error:", error);
      return [];
    }
  },

  /**
   * Lấy tất cả chat đang ACTIVE
   * GET /api/chat/staff/active
   */
  getAllActiveChats: async () => {
    try {
      const response = await api.get("/chat/staff/active");
      return response || [];
    } catch (error) {
      console.error("getAllActiveChats error:", error);
      throw error;
    }
  },

  /**
   * Lấy số lượng chat đang chờ
   * GET /api/chat/staff/queue/count
   */
  getWaitingCount: async () => {
    try {
      const response = await api.get("/chat/staff/queue/count");
      return response?.waitingCount ?? 0;
    } catch (error) {
      console.error("getWaitingCount error:", error);
      return 0;
    }
  },

  /**
   * Lấy chat của staff (bao gồm tất cả trạng thái)
   * GET /api/chat/staff/{staffUserId}/chats
   */
  getChatsByStaff: async (staffUserId) => {
    try {
      if (!staffUserId) return [];
      const response = await api.get(`/chat/staff/${staffUserId}/chats`);
      return response || [];
    } catch (error) {
      console.error("getChatsByStaff error:", error);
      throw error;
    }
  },

  /**
   * Lấy chat theo loại (admin)
   * GET /api/chat/staff/admin/type/{chatType}
   */
  getChatsByType: async (chatType) => {
    try {
      const response = await api.get(`/chat/staff/admin/type/${chatType}`);
      return response || [];
    } catch (error) {
      console.error("getChatsByType error:", error);
      return [];
    }
  },

  /**
   * Lấy tin nhắn của chat
   * GET /api/chat/staff/{chatId}/messages
   */
  getMessages: async (chatId) => {
    try {
      if (!chatId) return [];
      const response = await api.get(`/chat/staff/${chatId}/messages`);
      return response || [];
    } catch (error) {
      console.error("getMessages error:", error);
      throw error;
    }
  },

  /**
   * Gửi tin nhắn từ staff
   * POST /api/chat/staff/messages
   */
  sendMessage: async (chatId, userId, content) => {
    try {
      if (!chatId || !userId || !content || !content.trim()) {
        throw new Error("Missing required fields");
      }
      const payload = {
        chatId: Number(chatId),
        userId: Number(userId),
        message: content.trim(),
        isAi: false,
      };
      return await api.post("/chat/staff/messages", payload);
    } catch (error) {
      console.error("sendMessage error:", error);
      throw error;
    }
  },

  /**
   * Đánh dấu đã đọc
   * PUT /api/chat/staff/{chatId}/read/{userId}
   */
  markAsRead: async (chatId, userId) => {
    try {
      if (!chatId || !userId) return;
      return await api.put(`/chat/staff/${chatId}/read/${userId}`);
    } catch (error) {
      console.error("markAsRead error:", error);
      throw error;
    }
  },

  /**
   * Nhận chat (staff accept)
   * POST /api/chat/staff/{chatId}/accept/{staffUserId}
   */
  acceptChat: async (chatId, staffUserId) => {
    try {
      return await api.post(`/chat/staff/${chatId}/accept/${staffUserId}`);
    } catch (error) {
      console.error("acceptChat error:", error);
      throw error;
    }
  },

  /**
   * Chuyển chat cho nhân viên khác (admin)
   * POST /api/chat/staff/{chatId}/transfer?fromUserId={fromUserId}
   */
  transferChat: async (chatId, fromUserId, toUserId, reason = "") => {
    try {
      if (!chatId) throw new Error("chatId is required");
      if (!fromUserId) throw new Error("fromUserId is required");
      if (!toUserId) throw new Error("toUserId is required");

      const response = await api.post(
        `/chat/staff/${chatId}/transfer?fromUserId=${fromUserId}`,
        {
          toUserId: Number(toUserId),
          reason: reason || "Chuyển chat",
        },
      );
      return response;
    } catch (error) {
      console.error("transferChat error:", error);
      throw error;
    }
  },

  /**
   * Đóng chat
   * POST /api/chat/staff/{chatId}/close/{staffUserId}
   */
  closeChat: async (chatId, staffUserId) => {
    try {
      return await api.post(`/chat/staff/${chatId}/close/${staffUserId}`);
    } catch (error) {
      console.error("closeChat error:", error);
      throw error;
    }
  },

  /**
   * Mở lại chat
   * POST /api/chat/staff/{chatId}/reopen/{staffUserId}
   */
  reopenChat: async (chatId, staffUserId) => {
    try {
      return await api.post(`/chat/staff/${chatId}/reopen/${staffUserId}`);
    } catch (error) {
      console.error("reopenChat error:", error);
      throw error;
    }
  },
};

export default chatStaffService;
