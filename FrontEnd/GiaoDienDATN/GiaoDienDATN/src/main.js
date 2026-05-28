import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./assets/main.css";



// Import Bootstrap (nếu bạn dùng)
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

// Global error handler (tùy chọn)
app.config.errorHandler = (err, vm, info) => {
  console.error("Vue Error:", err);
  console.error("Error Info:", info);
};

// Global properties (tùy chọn)
app.config.globalProperties.$filters = {
  formatPrice(value) {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(value);
  },
};

app.mount("#app");
