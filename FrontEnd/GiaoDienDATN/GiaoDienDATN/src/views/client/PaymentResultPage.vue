<template>
  <div class="payment-result-page">
    <ClientHeader />

    <main class="result-container">
      <div v-if="status === 'success'" class="result-card success">
        <div class="icon-circle">
          <i class="fas fa-check"></i>
        </div>
        <h1>Thanh Toán Thành Công!</h1>
        <p>Cảm ơn bạn đã mua sắm. Đơn hàng của bạn đã được thanh toán qua VNPay.</p>
        <p v-if="txnRef">Mã giao dịch: <strong>{{ txnRef }}</strong></p>
        
        <div class="actions">
          <router-link to="/orders" class="btn btn-primary">Xem đơn hàng</router-link>
          <router-link to="/shop" class="btn btn-outline">Tiếp tục mua sắm</router-link>
        </div>
      </div>

      <div v-else class="result-card failed">
        <div class="icon-circle error">
          <i class="fas fa-times"></i>
        </div>
        <h1>Thanh Toán Thất Bại!</h1>
        <p>Rất tiếc, quá trình thanh toán của bạn không thành công hoặc đã bị hủy.</p>
        <p v-if="txnRef">Mã giao dịch: <strong>{{ txnRef }}</strong></p>
        
        <div class="actions">
          <router-link to="/checkout" class="btn btn-primary">Thử lại</router-link>
          <router-link to="/home" class="btn btn-outline">Về trang chủ</router-link>
        </div>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'

const route = useRoute()

// Query params from VNPay return endpoint
const status = computed(() => route.query.status || 'failed')
const txnRef = computed(() => route.query.txnRef || '')
</script>

<style scoped>
.payment-result-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}

.result-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  margin-top: 100px;
}

.result-card {
  background: white;
  padding: 50px 40px;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  text-align: center;
  max-width: 500px;
  width: 100%;
}

.icon-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #e8f5e9;
  color: #2e7d32;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  margin: 0 auto 24px;
}

.icon-circle.error {
  background: #ffebee;
  color: #c62828;
}

h1 {
  font-size: 28px;
  color: #2c3e50;
  margin-bottom: 16px;
}

p {
  color: #666;
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 32px;
}

.btn {
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s;
}

.btn-primary {
  background: #e63946;
  color: white;
  border: 2px solid #e63946;
}

.btn-primary:hover {
  background: #d62828;
  border-color: #d62828;
}

.btn-outline {
  background: transparent;
  color: #e63946;
  border: 2px solid #e63946;
}

.btn-outline:hover {
  background: #fff0f0;
}
</style>
