// src/stores/locationStore.js
import { defineStore } from "pinia";
import { provinceApi, wardApi } from "../api/locationApi";

export const useLocationStore = defineStore("location", {
  state: () => ({
    provinces: [],
    wards: [],
    loading: false,
    error: null,
  }),

  actions: {
    // ===== PROVINCE =====
    async fetchProvinces() {
      this.loading = true;
      try {
        const res = await provinceApi.getAll();
        this.provinces = res.data || [];
        this.error = null;
      } catch (error) {
        this.error = error.response?.data?.message || "Không thể tải dữ liệu";
        console.error("Error fetching provinces:", error);
      } finally {
        this.loading = false;
      }
    },

    async createProvince(data) {
      this.loading = true;
      try {
        const res = await provinceApi.create(data);
        await this.fetchProvinces();
        return res.data;
      } catch (error) {
        this.error = error.response?.data?.message || "Không thể thêm mới";
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async updateProvince(id, data) {
      this.loading = true;
      try {
        const res = await provinceApi.update(id, data);
        await this.fetchProvinces();
        return res.data;
      } catch (error) {
        this.error = error.response?.data?.message || "Không thể cập nhật";
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async deleteProvince(id) {
      this.loading = true;
      try {
        await provinceApi.delete(id);
        await this.fetchProvinces();
      } catch (error) {
        this.error = error.response?.data?.message || "Không thể xóa";
        throw error;
      } finally {
        this.loading = false;
      }
    },

    // ===== WARD =====
    async fetchWards(provinceId = null) {
      this.loading = true;
      try {
        let res;
        if (provinceId) {
          res = await wardApi.getByProvince(provinceId);
        } else {
          res = await wardApi.getAll();
        }
        this.wards = res.data || [];
        this.error = null;
      } catch (error) {
        this.error = error.response?.data?.message || "Không thể tải dữ liệu";
        console.error("Error fetching wards:", error);
      } finally {
        this.loading = false;
      }
    },

    async createWard(data) {
      this.loading = true;
      try {
        const res = await wardApi.create(data);
        await this.fetchWards();
        return res.data;
      } catch (error) {
        this.error = error.response?.data?.message || "Không thể thêm mới";
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async updateWard(id, data) {
      this.loading = true;
      try {
        const res = await wardApi.update(id, data);
        await this.fetchWards();
        return res.data;
      } catch (error) {
        this.error = error.response?.data?.message || "Không thể cập nhật";
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async deleteWard(id) {
      this.loading = true;
      try {
        await wardApi.delete(id);
        await this.fetchWards();
      } catch (error) {
        this.error = error.response?.data?.message || "Không thể xóa";
        throw error;
      } finally {
        this.loading = false;
      }
    },
  },
});
