import { sizeApi } from "../api/sizeApi";

export const sizeService = {
  async getSizes(page = 0, size = 10, keyword = "") {
    let response;
    if (keyword) {
      response = await sizeApi.search(keyword, { params: { page, size } });
    } else {
      response = await sizeApi.getAll({
        page,
        size,
      });
    }

    return response.data;
  },

  async getSize(id) {
    const response = await sizeApi.getById(id);
    return response.data;
  },

  async createSize(data) {
    const response = await sizeApi.create(data);
    return response.data;
  },

  async updateSize(id, data) {
    const response = await sizeApi.update(id, data);
    return response.data;
  },

  async deleteSize(id) {
    const response = await sizeApi.delete(id);
    return response.data;
  },

  async countSizes() {
    const response = await sizeApi.count();
    return response.data;
  },
};
