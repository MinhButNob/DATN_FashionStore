<template>
  <div class="about-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <!-- Breadcrumb -->
        <div class="breadcrumb">
          <router-link to="/">Trang chủ</router-link>
          <i class="fas fa-chevron-right"></i>
          <span>Về chúng tôi</span>
        </div>

        <!-- Hero Section -->
        <section class="hero-section">
          <h1>VỀ FASHION STORE</h1>
          <p>Thương hiệu thời trang hàng đầu Việt Nam</p>
        </section>

        <!-- Story Section -->
        <section class="story-section">
          <div class="story-content">
            <h2>Câu chuyện của chúng tôi</h2>
            <p>Fashion Store được thành lập vào năm 2020 với sứ mệnh mang đến những sản phẩm thời trang chất lượng cao với giá cả hợp lý cho mọi người. Chúng tôi tin rằng thời trang không chỉ là quần áo, mà còn là cách thể hiện cá tính và phong cách sống.</p>
            <p>Với đội ngũ thiết kế giàu kinh nghiệm và đam mê, chúng tôi luôn cập nhật những xu hướng mới nhất để mang đến cho khách hàng những trải nghiệm mua sắm tuyệt vời nhất.</p>
          </div>
          <div class="story-image">
            <i class="fas fa-store"></i>
          </div>
        </section>

        <!-- Mission Section -->
        <section class="mission-section">
          <h2>Sứ mệnh của chúng tôi</h2>
          <div class="mission-grid">
            <div class="mission-card">
              <i class="fas fa-heart"></i>
              <h3>Đam mê</h3>
              <p>Đam mê thời trang và luôn tìm kiếm những điều mới mẻ</p>
            </div>
            <div class="mission-card">
              <i class="fas fa-hand-holding-heart"></i>
              <h3>Tận tâm</h3>
              <p>Tận tâm với khách hàng và sản phẩm của mình</p>
            </div>
            <div class="mission-card">
              <i class="fas fa-star"></i>
              <h3>Chất lượng</h3>
              <p>Cam kết chất lượng tốt nhất cho mọi sản phẩm</p>
            </div>
          </div>
        </section>

        <!-- Values Section -->
        <section class="values-section">
          <h2>Giá trị cốt lõi</h2>
          <div class="values-list">
            <div class="value-item">
              <span class="value-number">01</span>
              <div class="value-content">
                <h3>Chất lượng hàng đầu</h3>
                <p>Chúng tôi chỉ chọn những nguyên liệu tốt nhất và quy trình sản xuất nghiêm ngặt để đảm bảo sản phẩm đạt chất lượng cao nhất.</p>
              </div>
            </div>
            <div class="value-item">
              <span class="value-number">02</span>
              <div class="value-content">
                <h3>Khách hàng là trung tâm</h3>
                <p>Mọi quyết định của chúng tôi đều dựa trên nhu cầu và mong muốn của khách hàng.</p>
              </div>
            </div>
            <div class="value-item">
              <span class="value-number">03</span>
              <div class="value-content">
                <h3>Phát triển bền vững</h3>
                <p>Chúng tôi cam kết phát triển bền vững, bảo vệ môi trường và hỗ trợ cộng đồng.</p>
              </div>
            </div>
          </div>
        </section>

        <!-- Team Section -->
        <section class="team-section">
          <h2>Đội ngũ của chúng tôi</h2>
          <div class="team-grid">
            <div v-for="member in team" :key="member.id" class="team-card">
              <div class="member-image">
                <i class="fas fa-user-circle"></i>
              </div>
              <h3>{{ member.name }}</h3>
              <p class="member-position">{{ member.position }}</p>
              <p class="member-desc">{{ member.description }}</p>
            </div>
          </div>
        </section>

        <!-- Stats Section -->
        <section class="stats-section">
          <div class="stat-card">
            <i class="fas fa-users"></i>
            <div class="stat-number">10K+</div>
            <div class="stat-label">Khách hàng</div>
          </div>
          <div class="stat-card">
            <i class="fas fa-tshirt"></i>
            <div class="stat-number">500+</div>
            <div class="stat-label">Sản phẩm</div>
          </div>
          <div class="stat-card">
            <i class="fas fa-truck"></i>
            <div class="stat-number">64</div>
            <div class="stat-label">Tỉnh thành</div>
          </div>
          <div class="stat-card">
            <i class="fas fa-award"></i>
            <div class="stat-number">5</div>
            <div class="stat-label">Giải thưởng</div>
          </div>
        </section>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'

const team = [
  {
    id: 1,
    name: 'Nguyễn Văn A',
    position: 'Giám đốc sáng tạo',
    description: 'Với hơn 10 năm kinh nghiệm trong ngành thời trang'
  },
  {
    id: 2,
    name: 'Trần Thị B',
    position: 'Trưởng phòng thiết kế',
    description: 'Tốt nghiệp trường thiết kế thời trang Paris'
  },
  {
    id: 3,
    name: 'Lê Văn C',
    position: 'Quản lý sản xuất',
    description: 'Chuyên gia về chất liệu và quy trình sản xuất'
  }
]
</script>

<style scoped>
.about-page {
  min-height: 100vh;
}

main {
  margin-top: 140px;
  padding: 20px 0 50px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 30px;
  color: var(--medium-gray);
  font-size: 14px;
}

.hero-section {
  text-align: center;
  padding: 60px 0;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  border-radius: 20px;
  margin-bottom: 50px;
  color: white;
}

.hero-section h1 {
  font-size: 48px;
  margin-bottom: 15px;
}

.hero-section p {
  font-size: 18px;
  opacity: 0.9;
}

.story-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 50px;
  align-items: center;
  margin-bottom: 50px;
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: var(--shadow);
}

.story-content h2 {
  color: var(--dark-gray);
  font-size: 32px;
  margin-bottom: 20px;
}

.story-content p {
  color: var(--medium-gray);
  line-height: 1.8;
  margin-bottom: 15px;
}

.story-image {
  background: linear-gradient(135deg, var(--light-gray), #e9ecef);
  border-radius: 15px;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.story-image i {
  font-size: 150px;
  color: var(--primary-red);
}

.mission-section {
  margin-bottom: 50px;
  text-align: center;
}

.mission-section h2 {
  color: var(--dark-gray);
  font-size: 32px;
  margin-bottom: 40px;
}

.mission-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.mission-card {
  background: white;
  padding: 40px 30px;
  border-radius: 15px;
  box-shadow: var(--shadow);
  transition: transform 0.3s;
}

.mission-card:hover {
  transform: translateY(-10px);
}

.mission-card i {
  font-size: 50px;
  color: var(--primary-red);
  margin-bottom: 20px;
}

.mission-card h3 {
  color: var(--dark-gray);
  margin-bottom: 10px;
  font-size: 20px;
}

.mission-card p {
  color: var(--medium-gray);
  line-height: 1.6;
}

.values-section {
  margin-bottom: 50px;
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: var(--shadow);
}

.values-section h2 {
  color: var(--dark-gray);
  font-size: 32px;
  margin-bottom: 40px;
  text-align: center;
}

.values-list {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.value-item {
  display: flex;
  gap: 30px;
  align-items: flex-start;
}

.value-number {
  font-size: 48px;
  font-weight: 800;
  color: var(--primary-red);
  opacity: 0.3;
  line-height: 1;
}

.value-content h3 {
  color: var(--dark-gray);
  font-size: 20px;
  margin-bottom: 10px;
}

.value-content p {
  color: var(--medium-gray);
  line-height: 1.6;
}

.team-section {
  margin-bottom: 50px;
}

.team-section h2 {
  color: var(--dark-gray);
  font-size: 32px;
  margin-bottom: 40px;
  text-align: center;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.team-card {
  background: white;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: var(--shadow);
  text-align: center;
  padding: 30px;
}

.member-image {
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
  background: var(--light-gray);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.member-image i {
  font-size: 60px;
  color: var(--primary-red);
}

.team-card h3 {
  color: var(--dark-gray);
  font-size: 18px;
  margin-bottom: 5px;
}

.member-position {
  color: var(--primary-red);
  font-weight: 600;
  margin-bottom: 15px;
}

.member-desc {
  color: var(--medium-gray);
  font-size: 14px;
  line-height: 1.6;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30px;
  background: linear-gradient(135deg, var(--dark-gray), #1a1e21);
  padding: 50px;
  border-radius: 20px;
  color: white;
}

.stat-card {
  text-align: center;
}

.stat-card i {
  font-size: 40px;
  color: var(--primary-red);
  margin-bottom: 15px;
}

.stat-number {
  font-size: 36px;
  font-weight: 800;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.8;
}

@media (max-width: 1024px) {
  .mission-grid,
  .team-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .hero-section h1 {
    font-size: 32px;
  }

  .story-section {
    grid-template-columns: 1fr;
    gap: 30px;
  }

  .mission-grid,
  .team-grid {
    grid-template-columns: 1fr;
  }

  .stats-section {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .value-item {
    flex-direction: column;
    gap: 15px;
  }

  .value-number {
    font-size: 36px;
  }
}
</style>