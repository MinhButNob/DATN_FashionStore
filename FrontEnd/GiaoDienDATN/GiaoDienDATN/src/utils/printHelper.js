import { formatPrice } from './helpers';

export const createPrintWindow = () => {
  const printWindow = window.open('', '_blank', 'width=400,height=600');
  if (!printWindow) {
    alert("Vui lòng cho phép popup để in hóa đơn (Look for popup blocker icon in URL bar)");
    return null;
  }
  printWindow.document.write("Đang chuẩn bị hóa đơn...");
  return printWindow;
};

export const printReceiptHTML = (invoice, printWindow) => {
  const invoiceNumber = invoice.posOrderInvoiceNumber || invoice.invoiceNumber || invoice.orderId || invoice.posOrderId;
  const customerName = invoice.posOrderCustomerName || invoice.customerName || 'Khách lẻ';
  const subtotal = invoice.posOrderSubtotal || invoice.subtotal || 0;
  const discount = invoice.posOrderDiscountAmount || invoice.discountAmount || 0;
  const total = invoice.posOrderTotalAmount || invoice.totalAmount || 0;
  const paid = invoice.posOrderPaidAmount || invoice.paidAmount || 0;
  const items = invoice.items || [];
  
  const now = new Date();
  const dateStr = now.toLocaleDateString('vi-VN');
  const timeStr = now.toLocaleTimeString('vi-VN');

  const itemsHTML = items.map(item => `
    <tr>
      <td style="padding: 5px 0; border-bottom: 1px dashed #ccc;">
        <div>${item.productName || 'Sản phẩm'} (${item.size || ''} - ${item.color || ''})</div>
        <div style="font-size: 11px; color: #555;">${item.quantity} x ${formatPrice(item.unitPrice || item.price || 0)}</div>
      </td>
      <td style="padding: 5px 0; border-bottom: 1px dashed #ccc; text-align: right; vertical-align: bottom;">
        ${formatPrice((item.unitPrice || item.price || 0) * item.quantity)}
      </td>
    </tr>
  `).join('');

  const html = `
    <!DOCTYPE html>
    <html lang="vi">
    <head>
      <meta charset="UTF-8">
      <title>Hóa đơn ${invoiceNumber}</title>
      <style>
        body { font-family: 'Courier New', Courier, monospace; font-size: 13px; color: #000; margin: 0; padding: 10px; width: 80mm; }
        .text-center { text-align: center; }
        .text-right { text-align: right; }
        .font-bold { font-weight: bold; }
        .header { margin-bottom: 15px; border-bottom: 1px dashed #000; padding-bottom: 10px; }
        .store-name { font-size: 18px; font-weight: bold; text-transform: uppercase; margin-bottom: 5px; }
        .info { margin-bottom: 15px; font-size: 12px; }
        .items { width: 100%; border-collapse: collapse; margin-bottom: 15px; }
        .summary { border-top: 1px dashed #000; padding-top: 10px; margin-bottom: 15px; }
        .summary-row { display: flex; justify-content: space-between; margin-bottom: 5px; }
        .total-row { font-size: 16px; font-weight: bold; margin-top: 5px; border-top: 1px dashed #000; padding-top: 5px; }
        .footer { text-align: center; font-size: 12px; border-top: 1px dashed #000; padding-top: 10px; }
        @media print {
          body { width: 100%; padding: 0; margin: 0; }
        }
      </style>
    </head>
    <body>
      <div class="header text-center">
        <div class="store-name">FASHION STORE</div>
        <div>ĐC: Số 1, Đường Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</div>
        <div>ĐT: 0123 456 789</div>
      </div>
      
      <div class="info">
        <div><span class="font-bold">Số phiếu:</span> ${invoiceNumber}</div>
        <div><span class="font-bold">Ngày:</span> ${dateStr} ${timeStr}</div>
        <div><span class="font-bold">Khách hàng:</span> ${customerName}</div>
      </div>

      <table class="items">
        ${itemsHTML}
      </table>

      <div class="summary">
        <div class="summary-row">
          <span>Tổng tiền hàng:</span>
          <span>${formatPrice(subtotal)}</span>
        </div>
        <div class="summary-row">
          <span>Chiết khấu:</span>
          <span>${formatPrice(discount)}</span>
        </div>
        <div class="summary-row total-row">
          <span>Khách cần trả:</span>
          <span>${formatPrice(total)}</span>
        </div>
        <div class="summary-row">
          <span>Khách thanh toán:</span>
          <span>${formatPrice(paid)}</span>
        </div>
      </div>

      <div class="footer">
        <div>Cảm ơn quý khách và hẹn gặp lại!</div>
        <div style="margin-top: 5px;">Powered by FashionStore POS</div>
      </div>
    </body>
    </html>
  `;

  if (printWindow) {
    printWindow.document.open();
    printWindow.document.write(html);
    printWindow.document.close();
    printWindow.focus();
    // setTimeout to allow rendering before print
    setTimeout(() => {
      printWindow.print();
      printWindow.close();
    }, 250);
  } else {
    // Fallback if not passed
    const fallbackWin = window.open('', '_blank', 'width=400,height=600');
    if (fallbackWin) {
      fallbackWin.document.open();
      fallbackWin.document.write(html);
      fallbackWin.document.close();
      fallbackWin.focus();
      setTimeout(() => {
        fallbackWin.print();
        fallbackWin.close();
      }, 250);
    } else {
      alert("Vui lòng cho phép popup để in hóa đơn");
    }
  }
};
