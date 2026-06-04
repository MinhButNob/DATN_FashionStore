import api from "./axios";

export const loginApi = async (email, password) => {
  const response = await api.post("/auth/login", {
    email,
    password,
  });

  return response.data;
};

export const registerApi = async (userData) => {
  const response = await api.post("/auth/register", {
    username: userData.name,
    email: userData.email,
    password: userData.password,
  });

  return response.data;
};
