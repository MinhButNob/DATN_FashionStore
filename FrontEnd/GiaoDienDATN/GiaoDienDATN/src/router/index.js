import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/authStore";

// Client views
import HomePage from "../views/client/HomePage.vue";
import ShopPage from "../views/client/ShopPage.vue";
import ProductDetailPage from "../views/client/ProductDetailPage.vue";
import CartPage from "../views/client/CartPage.vue";
import CheckoutPage from "../views/client/CheckoutPage.vue";
import ProfilePage from "../views/client/ProfilePage.vue";
import OrdersPage from "../views/client/OrdersPage.vue";
import WishlistPage from "../views/client/WishlistPage.vue";
import LoginPage from "../views/client/LoginPage.vue";
import SignupPage from "../views/client/SignupPage.vue";
import ForgotPasswordPage from "../views/client/ForgotPasswordPage.vue";
import ResetPasswordPage from "../views/client/ResetPasswordPage.vue";
import AboutPage from "../views/client/AboutPage.vue";
import ContactPage from "../views/client/ContactPage.vue";
import BlogDetailPage from "../views/client/BlogDetailPage.vue";
import NotFoundPage from "../views/client/NotFoundPage.vue";
import PaymentResultPage from "../views/client/PaymentResultPage.vue";

// Admin views
import AdminLayout from "../views/admin/AdminLayout.vue";
import AdminDashboard from "../views/admin/AdminDashboard.vue";
import AdminProducts from "../views/admin/AdminProducts.vue";
import AdminOrders from "../views/admin/AdminOrders.vue";
import AdminCategories from "../views/admin/AdminCategories.vue";
import AdminCategoryHierarchy from "../views/admin/Admincategoryhierarchy.vue";
import AdminCustomers from "../views/admin/AdminCustomers.vue";
import AdminSettings from "../views/admin/AdminSettings.vue";
import AdminLogin from "../views/admin/AdminLogin.vue";
import AdminSizes from "../views/admin/AdminSizes.vue";
import AdminImages from "../views/admin/AdminImages.vue";
import AdminEmployees from "../views/admin/AdminEmployees.vue";
import AdminRoles from "../views/admin/AdminRoles.vue";
import AdminColors from "../views/admin/AdminColors.vue";
import AdminPromotions from "../views/admin/AdminPromotions.vue";
import AdminVouchers from "../views/admin/AdminVouchers.vue";
import AdminBanners from "../views/admin/AdminBanners.vue";
import AdminReviews from "../views/admin/AdminReviews.vue";
import AdminStatistics from "../views/admin/AdminStatistics.vue";
// ✅ THÊM: Admin Chat
import AdminChat from "../views/admin/AdminChat.vue";

// Staff views
import StaffLayout from "../views/staff/StaffLayout.vue";
import StaffDashboard from "../views/staff/StaffDashboard.vue";
import StaffPos from "../views/staff/StaffPos.vue";
import StaffOrders from "../views/staff/StaffOrders.vue";
import StaffCustomers from "../views/staff/StaffCustomers.vue";
import StaffReturns from "../views/staff/StaffReturns.vue";
import StaffChat from "../views/staff/StaffChat.vue";

// ===== SALE STAFF VIEWS =====
import SaleStaffLayout from "../views/sale-staff/SaleStaffLayout.vue";
import SaleStaffDashboard from "../views/sale-staff/SaleStaffDashboard.vue";
import SaleStaffOrders from "../views/sale-staff/SaleStaffOrders.vue";
import SaleStaffCustomers from "../views/sale-staff/SaleStaffCustomers.vue";
import SaleStaffStock from "../views/sale-staff/SaleStaffStock.vue";
// ✅ THÊM: Sale Staff Chat
import SaleStaffChat from "../views/sale-staff/SaleStaffChat.vue";

const routes = [
  // ===== CLIENT ROUTES =====
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
    path: "/orders",
    name: "orders",
    component: OrdersPage,
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
    path: "/payment-result",
    name: "payment-result",
    component: PaymentResultPage,
  },

  // ===== ADMIN ROUTES =====
  {
    path: "/admin/login",
    name: "admin-login",
    component: AdminLogin,
    meta: { guest: true },
  },
  {
    path: "/admin",
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ["ADMIN"] },
    children: [
      { path: "", name: "admin-dashboard", component: AdminDashboard },
      { path: "products", name: "admin-products", component: AdminProducts },
      { path: "orders", name: "admin-orders", component: AdminOrders },
      {
        path: "categories",
        name: "admin-categories",
        component: AdminCategories,
      },
      {
        path: "category-hierarchy",
        name: "admin-category-hierarchy",
        component: AdminCategoryHierarchy,
      },
      { path: "sizes", name: "admin-sizes", component: AdminSizes },
      { path: "images", name: "admin-images", component: AdminImages },
      { path: "customers", name: "admin-customers", component: AdminCustomers },
      { path: "employees", name: "admin-employees", component: AdminEmployees },
      { path: "roles", name: "admin-roles", component: AdminRoles },
      { path: "colors", name: "admin-colors", component: AdminColors },
      { path: "reviews", name: "admin-reviews", component: AdminReviews },
      {
        path: "promotions",
        name: "admin-promotions",
        component: AdminPromotions,
      },
      { path: "banners", name: "admin-banners", component: AdminBanners },
      { path: "vouchers", name: "admin-vouchers", component: AdminVouchers },
      { path: "settings", name: "admin-settings", component: AdminSettings },
      {
        path: "statistics",
        name: "admin-statistics",
        component: AdminStatistics,
      },
      // ✅ THÊM: Admin Chat
      { path: "chat", name: "admin-chat", component: AdminChat },
    ],
  },

  // ===== STAFF ROUTES =====
  {
    path: "/staff",
    component: StaffLayout,
    meta: { requiresAuth: true, roles: ["STAFF"] },
    children: [
      { path: "", name: "staff-dashboard", component: StaffDashboard },
      { path: "pos", name: "staff-pos", component: StaffPos },
      { path: "orders", name: "staff-orders", component: StaffOrders },
      { path: "customers", name: "staff-customers", component: StaffCustomers },
      { path: "returns", name: "staff-returns", component: StaffReturns },
      { path: "chat", name: "staff-chat", component: StaffChat },
    ],
  },

  // ===== SALE STAFF ROUTES =====
  {
    path: "/sale-staff",
    component: SaleStaffLayout,
    meta: { requiresAuth: true, roles: ["SALE_STAFF", "ADMIN", "STAFF"] },
    children: [
      { path: "", name: "sale-staff-dashboard", component: SaleStaffDashboard },
      { path: "orders", name: "sale-staff-orders", component: SaleStaffOrders },
      {
        path: "customers",
        name: "sale-staff-customers",
        component: SaleStaffCustomers,
      },
      { path: "stock", name: "sale-staff-stock", component: SaleStaffStock },
      // ✅ THÊM: Sale Staff Chat
      { path: "chat", name: "sale-staff-chat", component: SaleStaffChat },
    ],
  },

  // ===== 404 NOT FOUND =====
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

// ===== NAVIGATION GUARDS =====
const getDefaultRouteByRole = (authStore) => {
  const roles = authStore.userRoles || [];

  if (roles.includes("ADMIN")) {
    return { name: "admin-dashboard" };
  }

  if (roles.includes("STAFF")) {
    return { name: "staff-dashboard" };
  }

  if (roles.includes("SALE_STAFF")) {
    return { name: "sale-staff-dashboard" };
  }

  return { name: "home" };
};

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: "login", query: { redirect: to.fullPath } });
    return;
  }

  if (to.meta.roles && authStore.isAuthenticated) {
    const userRoles = authStore.userRoles || [];
    const hasRequiredRole = to.meta.roles.some((role) =>
      userRoles.includes(role),
    );

    if (!hasRequiredRole) {
      next(getDefaultRouteByRole(authStore));
      return;
    }
  }

  if (to.meta.guest && authStore.isAuthenticated) {
    next(getDefaultRouteByRole(authStore));
    return;
  }

  next();
});

export default router;
