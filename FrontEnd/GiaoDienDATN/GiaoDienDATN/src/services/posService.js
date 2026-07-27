import api from "./api";

// =============================================
// PRODUCT APIs (dùng cho POS)
// =============================================
export const getPosProducts = (params = {}) => {
  return api.get("/products", { params: { size: 100, status: "ACTIVE", ...params } });
};

export const getProductById = (id) => {
  return api.get(`/products/${id}`);
};

export const getPosCategories = () => {
  return api.get("/categories/active");
};

// =============================================
// POS SESSION APIs
// =============================================
export const openPosSession = (data) => {
  return api.post("/pos/sessions/open", data);
};

export const closePosSession = (sessionId, employeeId) => {
  return api.put(`/pos/sessions/${sessionId}/close`, null, {
    params: { employeeId } });
};

export const getActiveSessions = () => {
  return api.get("/pos/sessions/active");
};

export const getMyActiveSession = async () => {
  const res = await api.get("/pos/sessions/my-active");

  console.log("SERVICE RESPONSE =", res);

  return res;
};

export const getActiveSessionByEmployee = (employeeId) => {
  return api.get(`/pos/sessions/employee/${employeeId}/active`);
};

export const getSessionDetail = (sessionId) => {
  return api.get(`/pos/sessions/${sessionId}`);
};

export const getSessionStats = (sessionId) => {
  return api.get(`/pos/sessions/${sessionId}/stats`);
};

export const posDepositCash = (data) => {
  return api.post("/pos/sessions/cash-transaction", { ...data, type: "DEPOSIT" });
};

export const posWithdrawCash = (data) => {
  return api.post("/pos/sessions/cash-transaction", { ...data, type: "WITHDRAW" });
};

// =============================================
// POS ORDER APIs
// =============================================
export const createPosOrder = (data) => {
  return api.post("/pos/orders", data);
};

export const getPosOrdersBySession = (sessionId) => {
  return api.get(`/pos/orders/session/${sessionId}`);
};

export const getPosOrderById = (orderId) => {
  return api.get(`/pos/orders/${orderId}`);
};

export const getPosOrderDetail = (orderId) => {
  return api.get(`/pos/orders/${orderId}/detail`);
};

export const printPosReceipt = (orderId, copies = 1) => {
  return api.post(`/pos/orders/${orderId}/print`, null, { params: { copies } });
};

// =============================================
// POS REFUND APIs
// =============================================
export const posRefundOrder = (data) => {
  return api.post("/pos/refunds", data);
};
