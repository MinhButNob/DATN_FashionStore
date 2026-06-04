<template>
  <div class="not-found-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <div class="not-found-content">
          <div class="error-code">404</div>
          <i class="fas fa-exclamation-triangle"></i>
          <h1>Không tìm thấy trang</h1>
          <p>Trang bạn đang tìm kiếm không tồn tại hoặc đã được di chuyển.</p>
          <div class="actions">
            <router-link to="/" class="btn btn-primary">
              <i class="fas fa-home"></i>
              Về trang chủ
            </router-link>
            <router-link to="/shop" class="btn btn-outline">
              <i class="fas fa-shopping-bag"></i>
              Tiếp tục mua sắm
            </router-link>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
}

main {
  margin-top: 140px;
  padding: 80px 0;
}

.not-found-content {
  text-align: center;
  max-width: 600px;
  margin: 0 auto;
  background: white;
  border-radius: 20px;
  padding: 60px 40px;
  box-shadow: var(--shadow);
}

.error-code {
  font-size: 120px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1;
  margin-bottom: 20px;
}

.not-found-content i {
  font-size: 60px;
  color: var(--primary-red);
  margin-bottom: 20px;
}

.not-found-content h1 {
  color: var(--dark-gray);
  font-size: 32px;
  margin-bottom: 15px;
}

.not-found-content p {
  color: var(--medium-gray);
  margin-bottom: 30px;
  font-size: 16px;
}

.actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .error-code {
    font-size: 80px;
  }

  .not-found-content h1 {
    font-size: 24px;
  }

  .actions {
    flex-direction: column;
  }

  .actions a {
    width: 100%;
  }
}
</style>