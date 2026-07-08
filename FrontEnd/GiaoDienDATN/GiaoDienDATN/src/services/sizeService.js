import { sizeApi } from "../api/sizeApi";

export const sizeService = {
  async getSizes(page = 0, size = 10, keyword = "", status = null) {
    let response;
    const trimmedKeyword = keyword ? keyword.trim() : "";
    if (trimmedKeyword) {
      response = await sizeApi.search(trimmedKeyword, page, size);
    } else {
      response = await sizeApi.getAll(page, size, status);
    }
    return response.data;
  },

  async getSize(id) {
    const response = await sizeApi.getById(id);
    return response.data;
  },

  async createSize(data) {
    // status đã là string "ACTIVE"/"INACTIVE" từ form select
    const payload = {
      sizeCode: data.sizeCode,
      sizeName: data.sizeName,
      description: data.description,
      status: typeof data.status === "boolean"
        ? (data.status ? "ACTIVE" : "INACTIVE")
        : (data.status || "ACTIVE"),
    };
    const response = await sizeApi.create(payload);
    return response.data;
  },

  async updateSize(id, data) {
    // status đã là string "ACTIVE"/"INACTIVE" từ form select
    const payload = {
      sizeCode: data.sizeCode,
      sizeName: data.sizeName,
      description: data.description,
      status: typeof data.status === "boolean"
        ? (data.status ? "ACTIVE" : "INACTIVE")
        : (data.status || "ACTIVE"),
    };
    const response = await sizeApi.update(id, payload);
    return response.data;
  },

  async deleteSize(id) {
    const response = await sizeApi.delete(id);
    return response.data;
  },

  async restoreSize(id) {
    const response = await sizeApi.restore(id);
    return response.data;
  },

  async countSizes() {
    const response = await sizeApi.count();
    return response.data;
  },
};
