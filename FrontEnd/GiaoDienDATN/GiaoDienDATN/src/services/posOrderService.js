import api from "./api";

const BASE_URL = "/pos/invoices";

export const getInvoices = (page = 0, size = 10) => {
  return api.get(BASE_URL, {
    params: { page, size }
  });
};

export const searchInvoices = (filter) => {
  return api.post(`${BASE_URL}/search`, filter);
};

export const getInvoiceDetail = (orderId) => {
  return api.get(`${BASE_URL}/${orderId}`);
};

export const getInvoiceByNumber = (invoiceNumber) => {
  return api.get(`${BASE_URL}/number/${invoiceNumber}`);
};

export const cancelInvoice = (orderId, reason) => {
  return api.put(
    `${BASE_URL}/${orderId}/cancel`,
    null,
    {
      params: { reason }
    }
  );
};

export const getStatistics = (startDate, endDate) => {
  return api.get(`${BASE_URL}/statistics`, {
    params: {
      startDate,
      endDate
    }
  });
};

export const printInvoiceApi = (orderId) => {
  return api.get(`${BASE_URL}/${orderId}/print`);
};