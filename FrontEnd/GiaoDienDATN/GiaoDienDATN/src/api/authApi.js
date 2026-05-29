import api from "./axios";

export default {
  // LOGIN
  login(data) {
    return api.post("/auth/login", data);
  },

  // REGISTER
  register(data) {
    return api.post("/auth/register", data);
  },

  // GOOGLE LOGIN
  googleLogin(idToken) {
    return api.post("/auth/google", {
      idToken,
    });
  },

  // FACEBOOK LOGIN
  facebookLogin(accessToken) {
    return api.post("/auth/facebook", {
      accessToken,
    });
  },

  // GET CURRENT USER
  getCurrentUser() {
    return api.get("/user/me");
  },
};
