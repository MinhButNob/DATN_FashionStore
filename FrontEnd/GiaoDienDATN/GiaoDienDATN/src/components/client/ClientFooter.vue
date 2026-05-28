<template>
  <footer>
    <div class="footer-top">
      <div class="container">
        <div class="footer-grid">
          <div class="footer-column">
            <h3>VỀ CHÚNG TÔI</h3>
            <p>Fashion Store là thương hiệu thời trang hàng đầu tại Việt Nam, mang đến những sản phẩm chất lượng và phong cách nhất.</p>
            <div class="social-links">
              <a href="#"><i class="fab fa-facebook-f"></i></a>
              <a href="#"><i class="fab fa-instagram"></i></a>
              <a href="#"><i class="fab fa-youtube"></i></a>
              <a href="#"><i class="fab fa-tiktok"></i></a>
            </div>
          </div>
          
          <div class="footer-column">
            <h3>LIÊN KẾT NHANH</h3>
            <ul>
              <li><router-link to="/">Trang chủ</router-link></li>
              <li><router-link to="/about">Giới thiệu</router-link></li>
              <li><router-link to="/shop">Sản phẩm</router-link></li>
              <li><router-link to="/blog">Tin tức</router-link></li>
              <li><router-link to="/contact">Liên hệ</router-link></li>
            </ul>
          </div>
          
          <div class="footer-column">
            <h3>HỖ TRỢ KHÁCH HÀNG</h3>
            <ul>
              <li><router-link to="/guide">Hướng dẫn mua hàng</router-link></li>
              <li><router-link to="/return">Chính sách đổi trả</router-link></li>
              <li><router-link to="/privacy">Chính sách bảo mật</router-link></li>
              <li><router-link to="/payment">Phương thức thanh toán</router-link></li>
              <li><router-link to="/faq">Câu hỏi thường gặp</router-link></li>
            </ul>
          </div>
          
          <div class="footer-column">
            <h3>THÔNG TIN LIÊN HỆ</h3>
            <ul>
              <li><i class="fas fa-map-marker-alt"></i> 123 Nguyễn Huệ, Q.1, TP.HCM</li>
              <li><i class="fas fa-phone"></i> 1900 1234</li>
              <li><i class="fas fa-envelope"></i> info@fashionstore.vn</li>
              <li><i class="fas fa-clock"></i> 8:00 - 22:00 hàng ngày</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
    
    <div class="footer-bottom">
      <div class="container">
        <p>&copy; {{ currentYear }} Fashion Store. All rights reserved.</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
footer {
  background: #212529;
  color: white;
  padding: 60px 0 20px;
  margin-top: 50px;
}

.footer-top {
  padding-bottom: 40px;
}

.footer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 40px;
}

.footer-column h3 {
  font-size: 18px;
  margin-bottom: 20px;
  position: relative;
  padding-bottom: 10px;
}

.footer-column h3::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 50px;
  height: 2px;
  background: var(--primary-red);
}

.footer-column p {
  color: #adb5bd;
  line-height: 1.6;
  margin-bottom: 20px;
}

.footer-column ul {
  list-style: none;
}

.footer-column ul li {
  margin-bottom: 12px;
}

.footer-column ul li a {
  color: #adb5bd;
  text-decoration: none;
  transition: color 0.3s;
}

.footer-column ul li a:hover {
  color: var(--primary-red);
  padding-left: 5px;
}

.footer-column ul li i {
  margin-right: 10px;
  color: var(--primary-red);
  width: 20px;
}

.social-links {
  display: flex;
  gap: 15px;
}

.social-links a {
  width: 36px;
  height: 36px;
  background: rgba(255,255,255,0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  text-decoration: none;
  transition: all 0.3s;
}

.social-links a:hover {
  background: var(--primary-red);
  transform: translateY(-3px);
}

.footer-bottom {
  border-top: 1px solid rgba(255,255,255,0.1);
  padding-top: 20px;
  text-align: center;
  color: #adb5bd;
  font-size: 14px;
}

@media (max-width: 768px) {
  .footer-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 30px;
  }
}

@media (max-width: 480px) {
  .footer-grid {
    grid-template-columns: 1fr;
  }
}
</style>