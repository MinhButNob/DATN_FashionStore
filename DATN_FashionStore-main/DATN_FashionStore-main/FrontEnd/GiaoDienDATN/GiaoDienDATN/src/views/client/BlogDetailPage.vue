<template>
  <div class="blog-detail-page">
    <ClientHeader />
    
    <main>
      <div class="container">
        <!-- Breadcrumb -->
        <div class="breadcrumb">
          <router-link to="/">Trang chủ</router-link>
          <i class="fas fa-chevron-right"></i>
          <router-link to="/blog">Blog</router-link>
          <i class="fas fa-chevron-right"></i>
          <span>{{ blogPost?.title }}</span>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
          <p>Đang tải bài viết...</p>
        </div>

        <!-- Not Found -->
        <div v-else-if="!blogPost" class="not-found">
          <i class="fas fa-exclamation-circle"></i>
          <h3>Không tìm thấy bài viết</h3>
          <router-link to="/blog" class="btn btn-primary">
            Quay lại Blog
          </router-link>
        </div>

        <!-- Blog Content -->
        <div v-else class="blog-content">
          <article>
            <div class="blog-header">
              <h1>{{ blogPost.title }}</h1>
              <div class="blog-meta">
                <span><i class="far fa-calendar-alt"></i> {{ blogPost.date }}</span>
                <span><i class="far fa-user"></i> {{ blogPost.author }}</span>
                <span><i class="far fa-folder"></i> {{ blogPost.category }}</span>
              </div>
            </div>

            <div class="blog-image">
              <i :class="blogPost.icon"></i>
            </div>

            <div class="blog-body" v-html="blogPost.content"></div>

            <div class="blog-tags">
              <span class="tag-label">Tags:</span>
              <span v-for="tag in blogPost.tags" :key="tag" class="tag">
                {{ tag }}
              </span>
            </div>

            <div class="blog-share">
              <span>Chia sẻ:</span>
              <button class="share-btn facebook">
                <i class="fab fa-facebook-f"></i>
              </button>
              <button class="share-btn twitter">
                <i class="fab fa-twitter"></i>
              </button>
              <button class="share-btn pinterest">
                <i class="fab fa-pinterest"></i>
              </button>
            </div>
          </article>

          <div class="blog-navigation">
            <button 
              class="nav-btn prev" 
              :disabled="!prevPost"
              @click="goToPost(prevPost?.id)"
            >
              <i class="fas fa-arrow-left"></i>
              <span>Bài trước</span>
            </button>
            <router-link to="/blog" class="nav-btn all">
              <i class="fas fa-th"></i>
              Tất cả bài viết
            </router-link>
            <button 
              class="nav-btn next" 
              :disabled="!nextPost"
              @click="goToPost(nextPost?.id)"
            >
              <span>Bài sau</span>
              <i class="fas fa-arrow-right"></i>
            </button>
          </div>

          <!-- Related Posts -->
          <div v-if="relatedPosts.length > 0" class="related-posts">
            <h2>Bài viết liên quan</h2>
            <div class="posts-grid">
              <div 
                v-for="post in relatedPosts" 
                :key="post.id" 
                class="post-card"
                @click="goToPost(post.id)"
              >
                <div class="post-image">
                  <i :class="post.icon"></i>
                </div>
                <div class="post-info">
                  <h3>{{ post.title }}</h3>
                  <p>{{ post.excerpt }}</p>
                  <span class="post-date">{{ post.date }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ClientHeader from '../../components/client/ClientHeader.vue'
import ClientFooter from '../../components/client/ClientFooter.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)

const blogPosts = ref([
  {
    id: 1,
    title: 'Xu hướng thời trang hè 2024',
    excerpt: 'Những xu hướng thời trang nổi bật nhất trong mùa hè năm nay...',
    date: '15/03/2024',
    author: 'Fashion Editor',
    category: 'Thời trang',
    icon: 'fas fa-tshirt',
    tags: ['thời trang', 'mùa hè', 'xu hướng'],
    content: `
      <p>Mùa hè 2024 đang đến gần với những xu hướng thời trang mới mẻ và đầy sáng tạo. Dưới đây là những xu hướng nổi bật nhất mà bạn không thể bỏ qua:</p>
      
      <h3>1. Phong cách Y2K trở lại</h3>
      <p>Những thiết kế mang phong cách những năm 2000 đang quay trở lại với các items như áo crop top, quần jeans ống loe, và phụ kiện màu sắc.</p>
      
      <h3>2. Màu sắc pastel</h3>
      <p>Các gam màu pastel nhẹ nhàng như hồng baby, xanh mint, vàng chanh sẽ là xu hướng chủ đạo của mùa hè này.</p>
      
      <h3>3. Chất liệu bền vững</h3>
      <p>Xu hướng thời trang bền vững tiếp tục được ưa chuộng với các chất liệu thân thiện môi trường như cotton hữu cơ, vải tái chế.</p>
    `
  },
  {
    id: 2,
    title: 'Cách phối đồ với giày thể thao',
    excerpt: 'Bí quyết phối đồ với giày thể thao cho nam và nữ...',
    date: '12/03/2024',
    author: 'Style Expert',
    category: 'Phong cách',
    icon: 'fas fa-shoe-prints',
    tags: ['giày', 'phối đồ', 'sneaker'],
    content: `
      <p>Giày thể thao không chỉ dành cho việc tập luyện mà còn là item thời trang không thể thiếu. Dưới đây là cách phối đồ với giày thể thao:</p>
      
      <h3>Phong cách năng động</h3>
      <p>Kết hợp giày thể thao với quần jogger và áo hoodie để có outfit thoải mái khi dạo phố.</p>
      
      <h3>Phong cách thanh lịch</h3>
      <p>Mix giày thể thao trắng với quần tây và áo blazer tạo nên vẻ ngoài lịch lãm nhưng vẫn trẻ trung.</p>
    `
  },
  {
    id: 3,
    title: 'Túi xách công sở cho nữ',
    excerpt: 'Lựa chọn túi xách phù hợp cho môi trường công sở...',
    date: '10/03/2024',
    author: 'Fashion Consultant',
    category: 'Phụ kiện',
    icon: 'fas fa-handbag',
    tags: ['túi xách', 'công sở', 'phụ kiện nữ'],
    content: `
      <p>Túi xách là phụ kiện không thể thiếu của phái đẹp khi đi làm. Cách chọn túi xách phù hợp:</p>
      
      <h3>Kích thước phù hợp</h3>
      <p>Nên chọn túi có kích thước vừa đủ để đựng laptop và đồ dùng cá nhân.</p>
      
      <h3>Màu sắc cơ bản</h3>
      <p>Các màu như đen, nâu, be dễ dàng phối với nhiều trang phục.</p>
      
      <h3>Chất liệu bền</h3>
      <p>Da thật hoặc da công nghiệp cao cấp sẽ bền và sang trọng hơn.</p>
    `
  }
])

const blogPost = computed(() => {
  const id = parseInt(route.params.id)
  return blogPosts.value.find(p => p.id === id)
})

const currentIndex = computed(() => {
  return blogPosts.value.findIndex(p => p.id === blogPost.value?.id)
})

const prevPost = computed(() => {
  if (currentIndex.value > 0) {
    return blogPosts.value[currentIndex.value - 1]
  }
  return null
})

const nextPost = computed(() => {
  if (currentIndex.value < blogPosts.value.length - 1) {
    return blogPosts.value[currentIndex.value + 1]
  }
  return null
})

const relatedPosts = computed(() => {
  if (!blogPost.value) return []
  return blogPosts.value
    .filter(p => p.id !== blogPost.value.id && p.category === blogPost.value.category)
    .slice(0, 3)
})

const goToPost = (id) => {
  if (id) {
    router.push(`/blog/${id}`)
  }
}

onMounted(() => {
  setTimeout(() => {
    loading.value = false
  }, 500)
})
</script>

<style scoped>
.blog-detail-page {
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

.breadcrumb a {
  color: var(--medium-gray);
  text-decoration: none;
  transition: color 0.3s;
}

.breadcrumb a:hover {
  color: var(--primary-red);
}

.loading,
.not-found {
  text-align: center;
  padding: 100px 20px;
  background: white;
  border-radius: 15px;
  box-shadow: var(--shadow);
}

.blog-content {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: var(--shadow);
}

.blog-header {
  margin-bottom: 30px;
}

.blog-header h1 {
  font-size: 36px;
  color: var(--dark-gray);
  margin-bottom: 15px;
}

.blog-meta {
  display: flex;
  gap: 20px;
  color: var(--medium-gray);
  font-size: 14px;
  flex-wrap: wrap;
}

.blog-meta i {
  margin-right: 5px;
  color: var(--primary-red);
}

.blog-image {
  height: 400px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
}

.blog-image i {
  font-size: 150px;
  color: white;
}

.blog-body {
  line-height: 1.8;
  color: var(--medium-gray);
  margin-bottom: 30px;
}

.blog-body h3 {
  color: var(--dark-gray);
  margin: 25px 0 15px;
  font-size: 24px;
}

.blog-body p {
  margin-bottom: 15px;
}

.blog-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 30px;
  padding: 20px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  flex-wrap: wrap;
}

.tag-label {
  font-weight: 600;
  color: var(--dark-gray);
}

.tag {
  padding: 5px 12px;
  background: var(--light-gray);
  border-radius: 20px;
  font-size: 13px;
  color: var(--medium-gray);
}

.blog-share {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 30px;
}

.blog-share span {
  font-weight: 600;
  color: var(--dark-gray);
  margin-right: 10px;
}

.share-btn {
  width: 35px;
  height: 35px;
  border: 1px solid var(--border-color);
  background: white;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s;
}

.share-btn:hover {
  transform: translateY(-3px);
}

.share-btn.facebook:hover {
  background: #1877f2;
  color: white;
  border-color: #1877f2;
}

.share-btn.twitter:hover {
  background: #1da1f2;
  color: white;
  border-color: #1da1f2;
}

.share-btn.pinterest:hover {
  background: #bd081c;
  color: white;
  border-color: #bd081c;
}

.blog-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 30px;
  gap: 15px;
  flex-wrap: wrap;
}

.nav-btn {
  padding: 12px 25px;
  background: var(--light-gray);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--dark-gray);
  text-decoration: none;
  font-weight: 500;
}

.nav-btn:hover:not(:disabled) {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.nav-btn.all {
  background: var(--primary-red);
  color: white;
  border-color: var(--primary-red);
}

.nav-btn.all:hover {
  background: var(--dark-red);
}

.related-posts {
  margin-top: 50px;
}

.related-posts h2 {
  color: var(--dark-gray);
  font-size: 24px;
  margin-bottom: 20px;
}

.posts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.post-card {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.post-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow);
}

.post-image {
  height: 150px;
  background: linear-gradient(135deg, var(--primary-red), var(--light-red));
  display: flex;
  align-items: center;
  justify-content: center;
}

.post-image i {
  font-size: 50px;
  color: white;
}

.post-info {
  padding: 15px;
}

.post-info h3 {
  color: var(--dark-gray);
  font-size: 16px;
  margin-bottom: 8px;
  line-height: 1.4;
}

.post-info p {
  color: var(--medium-gray);
  font-size: 13px;
  margin-bottom: 10px;
  line-height: 1.6;
}

.post-date {
  color: var(--primary-red);
  font-size: 12px;
}

@media (max-width: 768px) {
  main {
    margin-top: 200px;
  }

  .blog-header h1 {
    font-size: 28px;
  }

  .blog-image {
    height: 250px;
  }

  .blog-image i {
    font-size: 100px;
  }

  .posts-grid {
    grid-template-columns: 1fr;
  }

  .blog-navigation {
    flex-direction: column;
  }

  .nav-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>