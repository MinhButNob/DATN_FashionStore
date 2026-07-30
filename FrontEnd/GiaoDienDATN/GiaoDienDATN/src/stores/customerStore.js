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
    const customers = ref([])
    const totalElements = ref(0)
    const totalPages = ref(0)
    const currentPage = ref(0)
    const pageSize = ref(10)
    const loading = ref(false)
    const filterStatus = ref('')
    const statistics = ref({
        totalCustomers: 0,
        activeCustomers: 0,
        lockedCustomers: 0,
        deletedCustomers: 0,
        categoriesWithProducts: 0
    })

    const fetchCustomers = async (page = 0, size = 10, keyword = '', status = null) => {
        loading.value = true
        try {
            console.log('🔍 fetchCustomers - status nhận được:', status)
            
            const res = await getCustomers(
                page,
                size,
                keyword,
                'customerId',
                'asc',
                status
            )
            
            console.log('📥 Response từ API:', res)
            
            if (res && res.data) {
                customers.value = res.data.content || []
                totalElements.value = res.data.totalElements || 0
                totalPages.value = res.data.totalPages || 0
                currentPage.value = res.data.number || 0
            } else if (res && res.content) {
                customers.value = res.content || []
                totalElements.value = res.totalElements || 0
                totalPages.value = res.totalPages || 0
                currentPage.value = res.number || 0
            } else if (Array.isArray(res)) {
                customers.value = res
                totalElements.value = res.length
                totalPages.value = 1
                currentPage.value = 0
            } else {
                customers.value = []
                totalElements.value = 0
                totalPages.value = 0
                currentPage.value = 0
            }
            
            filterStatus.value = status || ''
            
            return { customers: customers.value, totalElements: totalElements.value }
        } catch (error) {
            console.error('Lỗi khi lấy danh sách:', error)
            throw error
        } finally {
            loading.value = false
        }
    }

    const fetchStatistics = async () => {
        try {
            const res = await getCustomerStatistics()
            if (res && res.data) {
                statistics.value = res.data
            } else if (res) {
                statistics.value = res
            }
            return statistics.value
        } catch (error) {
            console.error('Lỗi khi lấy thống kê:', error)
            throw error
        }
    }

    const addCustomer = async (data) => {
        try {
            await createCustomer(data)
            await fetchCustomers(currentPage.value, pageSize.value, '', filterStatus.value || null)
            await fetchStatistics()
        } catch (error) {
            console.error('Lỗi khi thêm:', error)
            throw error
        }
    }

    const editCustomer = async (id, data) => {
        try {
            await updateCustomer(id, data)
            await fetchCustomers(currentPage.value, pageSize.value, '', filterStatus.value || null)
        } catch (error) {
            console.error('Lỗi khi cập nhật:', error)
            throw error
        }
    }

    const removeCustomer = async (id) => {
        try {
            await deleteCustomer(id)
            await fetchCustomers(currentPage.value, pageSize.value, '', filterStatus.value || null)
            await fetchStatistics()
        } catch (error) {
            console.error('Lỗi khi xóa:', error)
            throw error
        }
    }

    const permanentDeleteCustomer = async (id) => {
        try {
            const { default: api } = await import('../services/api')
            await api.delete(`/admin/customers/${id}/permanent`)
            await fetchCustomers(currentPage.value, pageSize.value, '', filterStatus.value || null)
            await fetchStatistics()
        } catch (error) {
            console.error('Lỗi khi xóa vĩnh viễn:', error)
            throw error
        }
    }

    const restoreCustomer = async (id) => {
        try {
            const { default: api } = await import('../services/api')
            await api.patch(`/admin/customers/${id}/restore`)
            await fetchCustomers(currentPage.value, pageSize.value, '', filterStatus.value || null)
            await fetchStatistics()
        } catch (error) {
            console.error('Lỗi khi khôi phục:', error)
            throw error
        }
    }

    const toggleLock = async (id) => {
        try {
            await toggleLockCustomer(id)
            await fetchCustomers(currentPage.value, pageSize.value, '', filterStatus.value || null)
            await fetchStatistics()
        } catch (error) {
            console.error('Lỗi khi khóa/mở khóa:', error)
            throw error
        }
    }

    return {
        customers,
        totalElements,
        totalPages,
        currentPage,
        pageSize,
        loading,
        filterStatus,
        statistics,
        fetchCustomers,
        fetchStatistics,
        addCustomer,
        editCustomer,
        removeCustomer,
        permanentDeleteCustomer,
        restoreCustomer,
        toggleLock
    }
})