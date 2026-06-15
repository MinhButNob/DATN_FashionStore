# Plan to Fix POS Checkout Issues

1. **Save to `orders` and `order_details`:**
   Modify `PosService.createOrder()` to also create and save an `Order`, `OrderDetail` list, and `Payment` entity. We will map the POS order data to the central `Order` entities so they appear in the Admin orders list.

2. **Fix Session Balance / Inventory Deductions:**
   The user mentioned "tiền trong khi mở ca không bị trừ đi" (money in session not deducted). I suspect they either mean the total balance is not updating on the UI, or they mean the inventory (tồn kho) is not deducting properly.
   I will ensure that `Order` creation properly updates inventory and also update the UI in `StaffPos.vue` to show the current total cash (opening balance + sales).

Let's implement the backend changes first.
