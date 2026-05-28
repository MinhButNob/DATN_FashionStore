<template>
  <div class="contact-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <!-- Breadcrumb -->
        <div class="breadcrumb">
          <router-link to="/">Trang chủ</router-link>
          <i class="fas fa-chevron-right"></i>
          <span>Liên hệ</span>
        </div>

        <!-- Hero Section -->
        <section class="hero-section">
          <h1>LIÊN HỆ VỚI CHÚNG TÔI</h1>
          <p>Chúng tôi luôn sẵn sàng lắng nghe và hỗ trợ bạn</p>
        </section>

        <div class="contact-grid">
          <!-- Contact Form -->
          <div class="contact-form">
            <h2>Gửi tin nhắn cho chúng tôi</h2>
            <form @submit.prevent="submitForm">
              <div class="form-group">
                <i class="fas fa-user"></i>
                <input 
                  type="text" 
                  v-model="form.name" 
                  placeholder="Họ và tên"
                  required
                >
              </div>

              <div class="form-group">
                <i class="fas fa-envelope"></i>
                <input 
                  type="email" 
                  v-model="form.email" 
                  placeholder="Email"
                  required
                >
              </div>

              <div class="form-group">
                <i class="fas fa-phone"></i>
                <input 
                  type="tel" 
                  v-model="form.phone" 
                  placeholder="Số điện thoại"
                >
              </div>

              <div class="form-group">
                <i class="fas fa-tag"></i>
                <select v-model="form.subject">
                  <option value="">Chọn chủ đề</option>
                  <option value="product">Sản phẩm</option>
                  <option value="order">Đơn hàng</option>
                  <option value="support">Hỗ trợ kỹ thuật</option>
                  <option value="other">Khác</option>
                </select>
              </div>

              <div class="form-group">
                <i class="fas fa-comment"></i>
                <textarea 
                  v-model="form.message" 
                  placeholder="Nội dung tin nhắn"
                  rows="6"
                  required
                ></textarea>
              </div>

              <button type="submit" class="btn btn-primary" :disabled="submitting">
                <span v-if="!submitting">GỬI TIN NHẮN</span>
                <span v-else><i class="fas fa-spinner fa-spin"></i> Đang gửi...</span>
              </button>
            </form>
          </div>

          <!-- Contact Info -->
          <div class="contact-info">
            <h2>Thông tin liên hệ</h2>
            
            <div class="info-item">
              <i class="fas fa-map-marker-alt"></i>
              <div>
                <h3>Địa chỉ</h3>
                <p>123 Nguyễn Huệ, Quận 1, TP.HCM</p>
              </div>
            </div>

            <div class="info-item">
              <i class="fas fa-phone"></i>
              <div>
                <h3>Số điện thoại</h3>
                <p>1900 1234</p>
                <p>028 1234 5678</p>
              </div>
            </div>

            <div class="info-item">
              <i class="fas fa-envelope"></i>
              <div>
                <h3>Email</h3>
                <p>info@fashionstore.vn</p>
                <p>support@fashionstore.vn</p>
              </div>
            </div>

            <div class="info-item">
              <i class="fas fa-clock"></i>
              <div>
                <h3>Giờ làm việc</h3>
                <p>Thứ 2 - Thứ 6: 8:00 - 22:00</p>
                <p>Thứ 7 - Chủ nhật: 9:00 - 18:00</p>
              </div>
            </div>

            <div class="social-links">
              <h3>Kết nối với chúng tôi</h3>
              <div class="social-icons">
                <a href="#" class="facebook"><i class="fab fa-facebook-f"></i></a>
                <a href="#" class="instagram"><i class="fab fa-instagram"></i></a>
                <a href="#" class="youtube"><i class="fab fa-youtube"></i></a>
                <a href="#" class="tiktok"><i class="fab fa-tiktok"></i></a>
              </div>
            </div>
          </div>
        </div>

        <!-- Map -->
        <section class="map-section">
          <iframe 
            src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3919.424318242473!2d106.70166031480087!3d10.776400792320738!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x31752f49c53f7e6d%3A0x4c6c2e9c4c5b7f9!2zMTIzIE5ndXnhu4VuIEh14buHLCBC4bq_biBOZ2jDqSwgUXXhuq1uIDEsIFRow6BuaCBwaOG7kSBI4buTIENow60gTWluaCwgVmnhu4d0IE5hbQ!5e0!3m2!1svi!2s!4v1621234567890!5m2!1svi!2s" 
            width="100%" 
            height="450" 
            style="border:0;" 
            allowfullscreen="" 
            loading="lazy"
            title="Google Map"
          ></iframe>
        </section>

        <!-- FAQ Section -->
        <section class="faq-section">
          <h2>Câu hỏi thường gặp</h2>
          <div class="faq-grid">
            <div 
              v-for="(faq, index) in faqs" 
              :key="index" 
              class="faq-item"
              @click="toggleFaq(index)"
            >
              <div class="faq-question">
                <span>{{ faq.question }}</span>
                <i :class="activeFaq === index ? 'fas fa-chevron-up' : 'fas fa-chevron-down'"></i>
              </div>
              <div v-show="activeFaq === index" class="faq-answer">
                {{ faq.answer }}
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useNotification } from '../../composables/useNotification'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'

const { showNotification } = useNotification()

const form = ref({
  name: '',
  email: '',
  phone: '',
  subject: '',
  message: ''
})

const submitting = ref(false)

const faqs = [
  {
    question: 'Thời gian giao hàng mất bao lâu?',
    answer: 'Thời gian giao hàng từ 2-4 ngày làm việc đối với nội thành và 4-7 ngày đối với ngoại thành.'
  },
  {
    question: 'Làm thế nào để đổi trả sản phẩm?',
    answer: 'Bạn có thể đổi trả sản phẩm trong vòng 30 ngày kể từ ngày nhận hàng. Sản phẩm phải còn nguyên tem mác và chưa qua sử dụng.'
  },
  {
    question: 'Có được kiểm tra hàng trước khi thanh toán không?',
    answer: 'Có, bạn được phép kiểm tra hàng trước khi thanh toán. Nhân viên giao hàng sẽ hỗ trợ bạn kiểm tra sản phẩm.'
  },
  {
    question: 'Phương thức thanh toán nào được chấp nhận?',
    answer: 'Chúng tôi chấp nhận thanh toán bằng tiền mặt (COD), chuyển khoản ngân hàng, ví MoMo, VNPay.'
  },
  {
    question: 'Làm sao để theo dõi đơn hàng?',
    answer: 'Sau khi đặt hàng thành công, bạn sẽ nhận được email xác nhận và có thể theo dõi đơn hàng trong mục "Đơn hàng của tôi".'
  }
]

const activeFaq = ref(null)

const toggleFaq = (index) => {
  activeFaq.value = activeFaq.value === index ? null : index
}

const submitForm = () => {
  submitting.value = true
  
  // Simulate API call
  setTimeout(() => {
    submitting.value = false
    showNotification('Tin nhắn của bạn đã được gửi thành công!', 'success')
    
    // Reset form
    form.value = {
      name: '',
      email: '',
      phone: '',
      subject: '',
      message: ''
    }
  }, 1500)
}
</script>

<style scoped>
.contact-page {
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

.contact-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
  margin-bottom: 50px;
}

/* Contact Form */
.contact-form {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: var(--shadow);
}

.contact-form h2 {
  color: var(--dark-gray);
  font-size: 24px;
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

.form-group i:not(select) {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--medium-gray);
  z-index: 1;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px 12px 12px 45px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  font-size: 14px;
  transition: all 0.3s;
}

.form-group textarea {
  padding-top: 15px;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: var(--primary-red);
  box-shadow: 0 0 0 3px rgba(230, 57, 70, 0.1);
}

.form-group input:focus + i,
.form-group textarea:focus + i {
  color: var(--primary-red);
}

/* Contact Info */
.contact-info {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: var(--shadow);
}

.contact-info h2 {
  color: var(--dark-gray);
  font-size: 24px;
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
}

.info-item i {
  font-size: 24px;
  color: var(--primary-red);
  width: 40px;
}

.info-item h3 {
  color: var(--dark-gray);
  font-size: 16px;
  margin-bottom: 5px;
}

.info-item p {
  color: var(--medium-gray);
  margin-bottom: 3px;
  line-height: 1.6;
}

.social-links {
  margin-top: 30px;
  padding-top: 30px;
  border-top: 1px solid var(--border-color);
}

.social-links h3 {
  color: var(--dark-gray);
  font-size: 16px;
  margin-bottom: 15px;
}

.social-icons {
  display: flex;
  gap: 10px;
}

.social-icons a {
  width: 40px;
  height: 40px;
  border: 1px solid var(--border-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--medium-gray);
  transition: all 0.3s;
}

.social-icons a:hover {
  transform: translateY(-3px);
}

.social-icons .facebook:hover {
  background: #1877f2;
  color: white;
  border-color: #1877f2;
}

.social-icons .instagram:hover {
  background: linear-gradient(45deg, #f09433, #d62976, #962fbf);
  color: white;
  border-color: #d62976;
}

.social-icons .youtube:hover {
  background: #ff0000;
  color: white;
  border-color: #ff0000;
}

.social-icons .tiktok:hover {
  background: #000000;
  color: white;
  border-color: #000000;
}

/* Map Section */
.map-section {
  margin-bottom: 50px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: var(--shadow);
}

/* FAQ Section */
.faq-section {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: var(--shadow);
}

.faq-section h2 {
  color: var(--dark-gray);
  font-size: 24px;
  margin-bottom: 30px;
  text-align: center;
}

.faq-grid {
  max-width: 800px;
  margin: 0 auto;
}

.faq-item {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  margin-bottom: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.faq-item:hover {
  border-color: var(--primary-red);
}

.faq-question {
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--dark-gray);
}

.faq-question i {
  color: var(--primary-red);
  transition: transform 0.3s;
}

.faq-answer {
  padding: 0 20px 20px;
  color: var(--medium-gray);
  line-height: 1.6;
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .hero-section h1 {
    font-size: 32px;
  }

  .contact-grid {
    grid-template-columns: 1fr;
  }

  .contact-form,
  .contact-info {
    padding: 30px;
  }

  .info-item {
    flex-direction: column;
    gap: 10px;
    text-align: center;
  }

  .info-item i {
    margin: 0 auto;
  }

  .social-icons {
    justify-content: center;
  }
}
</style>