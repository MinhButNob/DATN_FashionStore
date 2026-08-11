import axios from 'axios';

const API_URL = 'http://localhost:8080/api/admin/dashboard';

class DashboardService {
    getDashboardKPIs() {
        return axios.get(API_URL, {
            headers: {
                Authorization: `Bearer ${localStorage.getItem('accessToken')}`
            }
        });
    }

    getMonthlyRevenue(year) {
        return axios.get(`${API_URL}/revenue/monthly`, {
            params: { year },
            headers: {
                Authorization: `Bearer ${localStorage.getItem('accessToken')}`
            }
        });
    }

    getDailyRevenue(year, month) {
        return axios.get(`${API_URL}/revenue/daily`, {
            params: { year, month },
            headers: {
                Authorization: `Bearer ${localStorage.getItem('accessToken')}`
            }
        });
    }

    getOrderStatus(filterType, dateRange, month, year) {
        const params = { filterType };
        if (filterType === 'Ngày' && dateRange) params.dateRange = dateRange;
        if (filterType === 'Tháng' && month) params.month = month;
        if (filterType === 'Năm' && year) params.year = year;
        
        return axios.get(`${API_URL}/order-status`, {
            params,
            headers: {
                Authorization: `Bearer ${localStorage.getItem('accessToken')}`
            }
        });
    }

    getInventoryAlerts(threshold = 10, limit = 5) {
        return axios.get(`${API_URL}/inventory-alerts`, {
            params: { threshold, limit },
            headers: {
                Authorization: `Bearer ${localStorage.getItem('accessToken')}`
            }
        });
    }
}

export default new DashboardService();
