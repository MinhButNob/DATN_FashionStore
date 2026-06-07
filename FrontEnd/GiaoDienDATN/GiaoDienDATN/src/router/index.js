import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/authStore";

// Client views
import HomePage from "../views/client/HomePage.vue";
import ShopPage from "../views/client/ShopPage.vue";
import ProductDetailPage from "../views/client/ProductDetailPage.vue";
import CartPage from "../views/client/CartPage.vue";
import CheckoutPage from "../views/client/CheckoutPage.vue";
import ProfilePage from "../views/client/ProfilePage.vue";
import WishlistPage from "../views/client/WishlistPage.vue";
import LoginPage from "../views/client/LoginPage.vue";
import SignupPage from "../views/client/SignupPage.vue";
import ForgotPasswordPage from "../views/client/ForgotPasswordPage.vue";
import ResetPasswordPage from "../views/client/ResetPasswordPage.vue";
import AboutPage from "../views/client/AboutPage.vue";
import ContactPage from "../views/client/ContactPage.vue";
import BlogDetailPage from "../views/client/BlogDetailPage.vue";
import NotFoundPage from "../views/client/NotFoundPage.vue";

// Admin views
import AdminLayout from "../views/admin/AdminLayout.vue";
import AdminDashboard from "../views/admin/AdminDashboard.vue";
import AdminProducts from "../views/admin/AdminProducts.vue";
import AdminOrders from "../views/admin/AdminOrders.vue";
import AdminCategories from "../views/admin/AdminCategories.vue";
import AdminCustomers from "../views/admin/AdminCustomers.vue";
import AdminSettings from "../views/admin/AdminSettings.vue";
import AdminLogin from "../views/admin/AdminLogin.vue";
import AdminSizes from "../views/admin/AdminSizes.vue";
import AdminImages from "../views/admin/AdminImages.vue";
import AdminEmployees from "../views/admin/AdminEmployees.vue";
import AdminRoles from "../views/admin/AdminRoles.vue";
import AdminPromotions from "../views/admin/AdminPromotions.vue"; // ← THÊM DÒNG NÀY

const routes = [
  { path: "/", redirect: "/home" },
  { path: "/home", name: "home", component: HomePage },
  { path: "/shop", name: "shop", component: ShopPage },
  {
    path: "/product/:id",
    name: "product-detail",
    component: ProductDetailPage,
  },
  { path: "/cart", name: "cart", component: CartPage },
  {
    path: "/checkout",
    name: "checkout",
    component: CheckoutPage,
    meta: { requiresAuth: true },
  },
  {
    path: "/profile",
    name: "profile",
    component: ProfilePage,
    meta: { requiresAuth: true },
  },
  {
    path: "/wishlist",
    name: "wishlist",
    component: WishlistPage,
    meta: { requiresAuth: true },
  },
  {
    path: "/login",
    name: "login",
    component: LoginPage,
    meta: { guest: true },
  },
  {
    path: "/signup",
    name: "signup",
    component: SignupPage,
    meta: { guest: true },
  },
  {
    path: "/forgot-password",
    name: "forgot-password",
    component: ForgotPasswordPage,
    meta: { guest: true },
  },
  {
    path: "/reset-password/:token",
    name: "reset-password",
    component: ResetPasswordPage,
    meta: { guest: true },
  },
  { path: "/about", name: "about", component: AboutPage },
  { path: "/contact", name: "contact", component: ContactPage },
  { path: "/blog/:id", name: "blog-detail", component: BlogDetailPage },
  {
    path: "/admin/login",
    name: "admin-login",
    component: AdminLogin,
    meta: { guest: true },
  },
  {
    path: "/admin",
    component: AdminLayout,
    meta: { requiresAdmin: true },
    children: [
      { path: "", name: "admin-dashboard", component: AdminDashboard },
      { path: "products", name: "admin-products", component: AdminProducts },
      { path: "orders", name: "admin-orders", component: AdminOrders },
      {
        path: "categories",
        name: "admin-categories",
        component: AdminCategories,
      },
      { path: "sizes", name: "admin-sizes", component: AdminSizes },
      { path: "images", name: "admin-images", component: AdminImages },
      { path: "customers", name: "admin-customers", component: AdminCustomers },
      { path: "employees", name: "admin-employees", component: AdminEmployees },
      { path: "roles", name: "admin-roles", component: AdminRoles },
      {
        path: "discounts",
        name: "admin-discounts",
        component: AdminPromotions,
      }, // ← THÊM DÒNG NÀY
      { path: "settings", name: "admin-settings", component: AdminSettings },
    ],
  },
  { path: "/:pathMatch(.*)*", name: "not-found", component: NotFoundPage },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition;
    return { top: 0 };
  },
});

// ========== NAVIGATION GUARDS ==========
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: "login", query: { redirect: to.fullPath } });
  } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    if (authStore.isAuthenticated) {
      next({ name: "home" });
    } else {
      next({ name: "admin-login", query: { redirect: to.fullPath } });
    }
  } else if (to.meta.guest && authStore.isAuthenticated) {
    next({ name: "home" });
  } else {
    next();
  }
});

export default router;
