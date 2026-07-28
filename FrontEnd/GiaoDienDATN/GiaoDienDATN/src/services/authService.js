import api from "./api";

export const authService = {
  login(email, password) {
    return api.post("/auth/login", { email, password });
  },

  register(userData) {
    return api.post("/auth/register", userData);
  },

  googleLogin(idToken) {
    return api.post("/auth/google", { idToken });
  },

  facebookLogin(accessToken) {
    return api.post("/auth/facebook", { accessToken });
  },

  getCurrentUser() {
    return api.get("/user/me");
  },

  updateProfile(data) {
    return api.put("/auth/profile", data);
  } };
