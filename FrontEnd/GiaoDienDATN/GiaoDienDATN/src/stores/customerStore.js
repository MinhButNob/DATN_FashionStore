import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
    getCustomers,
    createCustomer,
    updateCustomer,
    deleteCustomer,
    toggleLockCustomer,
    getCustomerStatistics
} from '../services/api'

export const useCustomerStore = defineStore('customer', () => {
    // State
    const customers = ref([])
    const totalElements = ref(0)
    const totalPages = ref(0)
    const currentPage = ref(0)
    const pageSize = ref(10)
    const loading = ref(false)
    const statistics = ref({
        totalCustomers: 0,
        activeCustomers: 0,
        lockedCustomers: 0,
        categoriesWithProducts: 0
    })

    // Actions
    const fetchCustomers = async (page = 0, size = 10, keyword = '') => {
        loading.value = true
        try {
            const res = await getCustomers(page, size, keyword)
            customers.value = res.content
            totalElements.value = res.totalElements
            totalPages.value = res.totalPages
            currentPage.value = res.number
            pageSize.value = res.size
            return res
        } catch (error) {
            console.error('Lỗi khi lấy danh sách khách hàng:', error)
            throw error
        } finally {
            loading.value = false
        }
    }

    const fetchStatistics = async () => {
        try {
            const res = await getCustomerStatistics()
            statistics.value = res
            return res
        } catch (error) {
            console.error('Lỗi khi lấy thống kê:', error)
            throw error
        }
    }

    const addCustomer = async (data) => {
        try {
            const res = await createCustomer(data)
            await fetchCustomers(currentPage.value, pageSize.value)
            await fetchStatistics()
            return res
        } catch (error) {
            console.error('Lỗi khi thêm khách hàng:', error)
            throw error
        }
    }

    const editCustomer = async (id, data) => {
        try {
            const res = await updateCustomer(id, data)
            await fetchCustomers(currentPage.value, pageSize.value)
            return res
        } catch (error) {
            console.error('Lỗi khi cập nhật khách hàng:', error)
            throw error
        }
    }

    const removeCustomer = async (id) => {
        try {
            await deleteCustomer(id)
            await fetchCustomers(currentPage.value, pageSize.value)
            await fetchStatistics()
        } catch (error) {
            console.error('Lỗi khi xóa khách hàng:', error)
            throw error
        }
    }

    const toggleLock = async (id) => {
        try {
            await toggleLockCustomer(id)
            await fetchCustomers(currentPage.value, pageSize.value)
            await fetchStatistics()
        } catch (error) {
            console.error('Lỗi khi khóa/mở khóa:', error)
            throw error
        }
    }

    return {
        // State
        customers,
        totalElements,
        totalPages,
        currentPage,
        pageSize,
        loading,
        statistics,
        // Actions
        fetchCustomers,
        fetchStatistics,
        addCustomer,
        editCustomer,
        removeCustomer,
        toggleLock
    }
})