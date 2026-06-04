import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./assets/main.css";
import vue3GoogleLogin from "vue3-google-login";


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
app.use(vue3GoogleLogin, {
  clientId:
    "402914017010-6lmb8tf0937e9gpmjfjh36ps6mf94lu6.apps.googleusercontent.com",
});

// Global properties (tùy chọn)
app.config.globalProperties.$filters = {
  formatPrice(value) {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(value);
  },
};

window.fbAsyncInit = function () {
  FB.init({
    appId: "1424131579521050",
    cookie: true,
    xfbml: true,
    version: "v23.0",
  });

  console.log("Facebook SDK loaded");
};

(function (d, s, id) {
  let js;
  const fjs = d.getElementsByTagName(s)[0];

  if (d.getElementById(id)) return;

  js = d.createElement(s);
  js.id = id;
  js.src = "https://connect.facebook.net/en_US/sdk.js";

  fjs.parentNode.insertBefore(js, fjs);
})(document, "script", "facebook-jssdk");

app.mount("#app");
