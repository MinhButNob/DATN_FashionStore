-- Tạo function cập nhật thời gian
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255),
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL'
        CHECK (provider IN ('LOCAL', 'GOOGLE', 'FACEBOOK')),
    provider_id VARCHAR(255),
    avatar_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED', 'DELETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uq_users_email_active
    ON users(LOWER(email))
    WHERE status <> 'DELETED' AND deleted_at IS NULL;

CREATE INDEX idx_users_status ON users(status);

CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uq_roles_name_active
    ON roles(role_name)
    WHERE status <> 'DELETED' AND deleted_at IS NULL;

CREATE TABLE user_roles (
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE RESTRICT
);

CREATE TABLE customers (
    customer_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE,
    customer_name VARCHAR(250) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE INDEX idx_customers_user_id ON customers(user_id);

CREATE TABLE employees (
    employee_id SERIAL PRIMARY KEY,
    employee_user_id INTEGER NOT NULL UNIQUE,
    employee_fullname VARCHAR(100) NOT NULL,
    employee_gender BOOLEAN NOT NULL,
    employee_phone VARCHAR(20) NOT NULL,
    employee_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    employee_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    employee_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (employee_user_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE INDEX idx_employees_user_id ON employees(employee_user_id);

CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    category_description TEXT,
    category_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (category_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    category_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category_deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uq_categories_name_active
    ON categories(category_name)
    WHERE category_status <> 'DELETED' AND category_deleted_at IS NULL;

CREATE INDEX idx_categories_status ON categories(category_status);

CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    product_description TEXT,
    product_category_id INTEGER,
    product_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (product_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    product_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (product_category_id) REFERENCES categories(category_id) ON DELETE SET NULL
);

CREATE INDEX idx_products_category_id ON products(product_category_id);
CREATE INDEX idx_products_status ON products(product_status);
CREATE INDEX idx_products_not_deleted ON products(product_category_id) WHERE product_deleted_at IS NULL;

CREATE TABLE sizes (
    size_id SERIAL PRIMARY KEY,
    size_code VARCHAR(20) NOT NULL,
    size_name VARCHAR(50) NOT NULL,
    size_description TEXT,
    size_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (size_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    size_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    size_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    size_deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uq_sizes_code_active ON sizes(size_code) WHERE size_status <> 'DELETED' AND size_deleted_at IS NULL;
CREATE INDEX idx_sizes_status ON sizes(size_status);

CREATE TABLE colors (
    color_id SERIAL PRIMARY KEY,
    color_code VARCHAR(20) NOT NULL,
    color_name VARCHAR(50) NOT NULL,
    color_hex_code VARCHAR(20),
    color_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (color_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    color_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    color_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    color_deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uq_colors_code_active ON colors(color_code) WHERE color_status <> 'DELETED' AND color_deleted_at IS NULL;
CREATE UNIQUE INDEX uq_colors_name_active ON colors(color_name) WHERE color_status <> 'DELETED' AND color_deleted_at IS NULL;
CREATE INDEX idx_colors_status ON colors(color_status);

CREATE TABLE product_details (
    product_detail_id SERIAL PRIMARY KEY,
    product_detail_product_id INTEGER NOT NULL,
    product_detail_color_id INTEGER NOT NULL,
    product_detail_size_id INTEGER NOT NULL,
    product_detail_sku VARCHAR(50) NOT NULL,
    product_detail_price DECIMAL(15,2) NOT NULL CHECK (product_detail_price >= 0),
    product_detail_quantity INTEGER NOT NULL DEFAULT 0 CHECK (product_detail_quantity >= 0),
    product_detail_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (product_detail_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    product_detail_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_detail_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_detail_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (product_detail_product_id) REFERENCES products(product_id) ON DELETE RESTRICT,
    FOREIGN KEY (product_detail_color_id) REFERENCES colors(color_id) ON DELETE RESTRICT,
    FOREIGN KEY (product_detail_size_id) REFERENCES sizes(size_id) ON DELETE RESTRICT
);

CREATE INDEX idx_product_details_sku ON product_details(product_detail_sku);
CREATE INDEX idx_product_details_product_id ON product_details(product_detail_product_id);
CREATE INDEX idx_product_details_status ON product_details(product_detail_status);
CREATE INDEX idx_product_details_not_deleted ON product_details(product_detail_product_id) WHERE product_detail_deleted_at IS NULL;

CREATE UNIQUE INDEX uq_product_details_composite_active
    ON product_details(product_detail_product_id, product_detail_color_id, product_detail_size_id)
    WHERE product_detail_deleted_at IS NULL;

CREATE TABLE product_images (
    product_image_id SERIAL PRIMARY KEY,
    product_image_product_detail_id INTEGER NOT NULL,
    product_image_path VARCHAR(255) NOT NULL,
    product_image_type VARCHAR(50),
    product_image_display_order INTEGER NOT NULL DEFAULT 0,
    product_image_is_main BOOLEAN NOT NULL DEFAULT FALSE,
    product_image_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (product_image_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    product_image_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_image_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_image_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (product_image_product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE RESTRICT
);

CREATE INDEX idx_product_images_detail ON product_images(product_image_product_detail_id);
CREATE INDEX idx_product_images_main_active ON product_images(product_image_product_detail_id) 
    WHERE product_image_is_main = TRUE AND product_image_deleted_at IS NULL;
CREATE INDEX idx_product_images_display_order ON product_images(product_image_product_detail_id, product_image_display_order)
    WHERE product_image_deleted_at IS NULL;
CREATE INDEX idx_product_images_not_deleted ON product_images(product_image_product_detail_id, product_image_display_order) 
    WHERE product_image_deleted_at IS NULL;

CREATE TABLE promotions (
    promotion_id SERIAL PRIMARY KEY,
    promotion_name VARCHAR(100) NOT NULL,
    promotion_description TEXT,
    promotion_start_date TIMESTAMP NOT NULL,
    promotion_end_date TIMESTAMP NOT NULL,
    promotion_discount_percent DECIMAL(5,2)
        CHECK (promotion_discount_percent IS NULL OR promotion_discount_percent BETWEEN 0 AND 100),
    promotion_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (promotion_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    promotion_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    promotion_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    promotion_deleted_at TIMESTAMP NULL,
    CHECK (promotion_end_date > promotion_start_date)
);

CREATE INDEX idx_promotions_status ON promotions(promotion_status);
CREATE INDEX idx_promotions_active_dates ON promotions(promotion_start_date, promotion_end_date)
    WHERE promotion_status = 'ACTIVE' AND promotion_deleted_at IS NULL;

CREATE TABLE product_promotions (
 	product_promotion_id SERIAL PRIMARY KEY,
    product_promotion_product_detail_id INTEGER NOT NULL,
    product_promotion_promotion_id INTEGER NOT NULL,
    FOREIGN KEY (product_promotion_product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE RESTRICT,
    FOREIGN KEY (product_promotion_promotion_id) REFERENCES promotions(promotion_id) ON DELETE RESTRICT,
    UNIQUE(product_promotion_product_detail_id, product_promotion_promotion_id)
);

CREATE TABLE vouchers (
    voucher_id SERIAL PRIMARY KEY,
    voucher_code VARCHAR(50) NOT NULL,
    voucher_discount_amount DECIMAL(15,2),
    voucher_discount_percent DECIMAL(5,2)
        CHECK (voucher_discount_percent IS NULL OR voucher_discount_percent BETWEEN 0 AND 100),
    voucher_start_date TIMESTAMP NOT NULL,
    voucher_end_date TIMESTAMP NOT NULL,
    voucher_max_usage INTEGER CHECK (voucher_max_usage IS NULL OR voucher_max_usage >= 0),
    voucher_used_count INTEGER NOT NULL DEFAULT 0 CHECK (voucher_used_count >= 0),
    voucher_min_order_value DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (voucher_min_order_value >= 0),
    voucher_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (voucher_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    voucher_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voucher_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voucher_deleted_at TIMESTAMP NULL,
    CHECK (
        (voucher_discount_amount IS NOT NULL AND voucher_discount_percent IS NULL) OR
        (voucher_discount_amount IS NULL AND voucher_discount_percent IS NOT NULL)
    ),
    CHECK (voucher_discount_amount IS NULL OR voucher_discount_amount >= 0),
    CHECK (voucher_end_date > voucher_start_date)
);

CREATE UNIQUE INDEX uq_vouchers_code_active ON vouchers(voucher_code) WHERE voucher_status <> 'DELETED' AND voucher_deleted_at IS NULL;
CREATE INDEX idx_vouchers_status ON vouchers(voucher_status);


CREATE TABLE carts (
    cart_id SERIAL PRIMARY KEY,
    cart_customer_id INTEGER NOT NULL,
    voucher_id INTEGER,
    voucher_discount DECIMAL(15,2),
    cart_quantity INTEGER CHECK (cart_quantity > 0),
    cart_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cart_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cart_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (cart_customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id) ON DELETE RESTRICT,
    UNIQUE (cart_customer_id, voucher_id)
);

CREATE INDEX idx_carts_customer_id ON carts(cart_customer_id);

CREATE TABLE cart_items (
    cart_item_id SERIAL PRIMARY KEY,
    cart_item_cart_id INTEGER NOT NULL,
    cart_item_product_detail_id INTEGER NOT NULL,
    cart_item_quantity INTEGER CHECK (cart_item_quantity > 0),
    discounted_price DECIMAL(15,2),
    subtotal DECIMAL(15,2),
    cart_item_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cart_item_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cart_item_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (cart_item_cart_id) REFERENCES carts(cart_id) ON DELETE CASCADE,
    FOREIGN KEY (cart_item_product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE RESTRICT,
    UNIQUE (cart_item_cart_id, cart_item_product_detail_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_item_cart_id);
CREATE INDEX idx_cart_items_product_detail_id ON cart_items(cart_item_product_detail_id);

CREATE TABLE shipping_addresses (
     shipping_address_id SERIAL PRIMARY KEY,
    shipping_address_customer_id INTEGER,
    shipping_address_full_name VARCHAR(100) NOT NULL,
    shipping_address_phone VARCHAR(20),
    shipping_address_address_line TEXT NOT NULL,
    shipping_address_city VARCHAR(50),
    shipping_address_district VARCHAR(50),
    shipping_address_ward VARCHAR(50),
    shipping_address_postal_code VARCHAR(20),
    shipping_address_is_default BOOLEAN NOT NULL DEFAULT FALSE,
    shipping_address_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (shipping_address_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    shipping_address_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shipping_address_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shipping_address_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (shipping_address_customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_shipping_addresses_default_active 
    ON shipping_addresses(shipping_address_customer_id)
    WHERE shipping_address_is_default = TRUE AND shipping_address_deleted_at IS NULL;

CREATE INDEX idx_shipping_addresses_customer ON shipping_addresses(shipping_address_customer_id) WHERE shipping_address_deleted_at IS NULL;
CREATE INDEX idx_shipping_addresses_customer_id ON shipping_addresses(shipping_address_customer_id);

CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    order_customer_id INTEGER,
    order_shipping_address_id INTEGER,
    order_voucher_id INTEGER,
    order_cart_id INTEGER,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_status VARCHAR(100) NOT NULL DEFAULT 'PENDING_PAYMENT'
        CHECK (order_status IN ('PENDING_PAYMENT', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'RETURNED', 'DELETED')),
    order_subtotal_amount DECIMAL(15,2) NOT NULL CHECK (order_subtotal_amount >= 0),
    order_discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (order_discount_amount >= 0),
    order_shipping_fee DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (order_shipping_fee >= 0),
    order_total_amount DECIMAL(15,2) GENERATED ALWAYS AS 
        (order_subtotal_amount - order_discount_amount + order_shipping_fee) STORED,
    order_payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' 
        CHECK (order_payment_status IN ('UNPAID', 'PAID', 'PARTIAL', 'REFUNDED', 'DELETED')),
    order_notes TEXT,
    order_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (order_customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL,
    FOREIGN KEY (order_cart_id) REFERENCES carts(cart_id) ON DELETE SET NULL,
    FOREIGN KEY (order_shipping_address_id) REFERENCES shipping_addresses(shipping_address_id) ON DELETE SET NULL,
    FOREIGN KEY (order_voucher_id) REFERENCES vouchers(voucher_id) ON DELETE SET NULL
);

CREATE INDEX idx_orders_customer ON orders(order_customer_id) WHERE order_deleted_at IS NULL;
CREATE INDEX idx_orders_status ON orders(order_status) WHERE order_deleted_at IS NULL;
CREATE INDEX idx_orders_date ON orders(order_date) WHERE order_deleted_at IS NULL;
CREATE INDEX idx_orders_payment_status ON orders(order_payment_status) WHERE order_deleted_at IS NULL;
CREATE INDEX idx_orders_customer_id ON orders(order_customer_id);
CREATE INDEX idx_orders_deleted ON orders(order_deleted_at) WHERE order_deleted_at IS NULL;
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_orders_cart_id ON orders(order_cart_id) WHERE order_deleted_at IS NULL;

CREATE TABLE order_details (
    order_detail_id SERIAL PRIMARY KEY,
    order_detail_order_id INTEGER NOT NULL,
    order_detail_product_detail_id INTEGER NOT NULL,
    order_detail_unit_price DECIMAL(15,2) NOT NULL CHECK (order_detail_unit_price >= 0),
    order_detail_discount DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (order_detail_discount >= 0),
    order_detail_final_amount DECIMAL(15,2) NOT NULL CHECK (order_detail_final_amount >= 0),
    order_detail_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_detail_order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (order_detail_product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE RESTRICT
);

CREATE INDEX idx_order_details_order ON order_details(order_detail_order_id);
CREATE INDEX idx_order_details_product ON order_details(order_detail_product_detail_id);
CREATE INDEX idx_order_details_order_id ON order_details(order_detail_order_id);
CREATE INDEX idx_order_details_product_detail_id ON order_details(order_detail_product_detail_id);

CREATE TABLE order_histories (
    history_id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL,
    order_history_order_status VARCHAR(100),
    order_history_note TEXT,
    order_history_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_history_deleted_at TIMESTAMP NULL,  -- ✅ THÊM CỘT NÀY
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
);

CREATE INDEX idx_order_histories_order ON order_histories(order_id);
CREATE INDEX idx_order_histories_timestamp ON order_histories(order_history_timestamp);
CREATE INDEX idx_order_histories_order_id ON order_histories(order_id);
CREATE INDEX idx_order_histories_status ON order_histories(order_history_order_status) WHERE order_history_deleted_at IS NULL;
CREATE TABLE payment_methods (
    payment_method_id SERIAL PRIMARY KEY,
    method_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uq_payment_methods_name_active ON payment_methods(method_name) WHERE deleted_at IS NULL;

CREATE TABLE payments (
     payment_id SERIAL PRIMARY KEY,
    payment_order_id INTEGER NOT NULL,
    payment_method_id INTEGER NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_amount DECIMAL(15,2) NOT NULL CHECK (payment_amount >= 0),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'CANCELLED', 'DELETED')),
    payment_transaction_id VARCHAR(50),
    payment_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (payment_order_id) REFERENCES orders(order_id) ON DELETE RESTRICT,
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(payment_method_id) ON DELETE RESTRICT
);

CREATE INDEX idx_payments_order ON payments(payment_order_id) WHERE payment_deleted_at IS NULL;
CREATE INDEX idx_payments_status ON payments(payment_status) WHERE payment_deleted_at IS NULL;
CREATE INDEX idx_payments_order_id ON payments(payment_order_id);
CREATE INDEX idx_payments_status_idx ON payments(payment_status);

CREATE TABLE payment_transactions (
    payment_transaction_id SERIAL PRIMARY KEY,
    payment_id INTEGER NOT NULL,
    transaction_type VARCHAR(20) NOT NULL
        CHECK (transaction_type IN ('PAYMENT', 'REFUND', 'CANCEL', 'ADJUSTMENT')),
    amount DECIMAL(15,2) NOT NULL CHECK (amount >= 0),
    bank_transaction_code VARCHAR(50),
    transaction_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'DELETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_transactions_payment ON payment_transactions(payment_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_payment_transactions_type ON payment_transactions(transaction_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions(payment_id);

CREATE TABLE invoices (
    invoice_id SERIAL PRIMARY KEY,
    invoice_order_id INTEGER NOT NULL,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_status VARCHAR(20) NOT NULL DEFAULT 'ISSUED'
        CHECK (invoice_status IN ('ISSUED', 'CANCELLED', 'REFUNDED', 'DELETED')),
    invoice_issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invoice_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invoice_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invoice_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (invoice_order_id) REFERENCES orders(order_id) ON DELETE RESTRICT
);

CREATE INDEX idx_invoices_order ON invoices(invoice_order_id) WHERE invoice_deleted_at IS NULL;
CREATE INDEX idx_invoices_number ON invoices(invoice_number) WHERE invoice_deleted_at IS NULL;
CREATE INDEX idx_invoices_status ON invoices(invoice_status) WHERE invoice_deleted_at IS NULL;

CREATE TABLE voucher_usage (
    voucher_usage_id SERIAL PRIMARY KEY,
    voucher_id INTEGER NOT NULL,
    order_id INTEGER NOT NULL,
    customer_id INTEGER,
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    discount_applied DECIMAL(15,2) NOT NULL CHECK (discount_applied >= 0),
    voucher_usage_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voucher_usage_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voucher_usage_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id) ON DELETE RESTRICT,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_voucher_usage_order_active 
    ON voucher_usage(voucher_id, order_id)
    WHERE voucher_usage_deleted_at IS NULL;

CREATE INDEX idx_voucher_usage_voucher ON voucher_usage(voucher_id) WHERE voucher_usage_deleted_at IS NULL;
CREATE INDEX idx_voucher_usage_customer ON voucher_usage(customer_id) WHERE voucher_usage_deleted_at IS NULL;
CREATE INDEX idx_voucher_usage_order ON voucher_usage(order_id) WHERE voucher_usage_deleted_at IS NULL;
CREATE INDEX idx_voucher_usage_voucher_id ON voucher_usage(voucher_id);
CREATE INDEX idx_voucher_usage_customer_id ON voucher_usage(customer_id);

CREATE TABLE chats (
    chat_id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    admin_id INTEGER,
    chat_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (chat_status IN ('ACTIVE', 'CLOSED', 'DELETED')),
    chat_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    chat_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    chat_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    FOREIGN KEY (admin_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_chats_customer_active ON chats(customer_id) WHERE chat_status = 'ACTIVE' AND chat_deleted_at IS NULL;
CREATE INDEX idx_chats_customer ON chats(customer_id) WHERE chat_deleted_at IS NULL;
CREATE INDEX idx_chats_admin ON chats(admin_id) WHERE chat_deleted_at IS NULL;
CREATE INDEX idx_chats_status ON chats(chat_status) WHERE chat_deleted_at IS NULL;
CREATE INDEX idx_chats_customer_id ON chats(customer_id);

CREATE TABLE messages (
    message_id SERIAL PRIMARY KEY,
    message_chat_id INTEGER NOT NULL,
    message_user_id INTEGER NOT NULL,
    message_content TEXT NOT NULL,
    message_sent_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message_is_read BOOLEAN NOT NULL DEFAULT FALSE,
    message_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (message_status IN ('ACTIVE', 'DELETED')),
    message_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (message_chat_id) REFERENCES chats(chat_id) ON DELETE CASCADE,
    FOREIGN KEY (message_user_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE INDEX idx_messages_chat ON messages(message_chat_id) WHERE message_deleted_at IS NULL;
CREATE INDEX idx_messages_user ON messages(message_user_id) WHERE message_deleted_at IS NULL;
CREATE INDEX idx_messages_sent_date ON messages(message_sent_date) WHERE message_deleted_at IS NULL;
CREATE INDEX idx_messages_is_read ON messages(message_is_read) WHERE message_deleted_at IS NULL;
CREATE INDEX idx_messages_chat_id ON messages(message_chat_id);

CREATE TABLE notifications (
    notification_id SERIAL PRIMARY KEY,
    notification_user_id INTEGER NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    notification_content TEXT NOT NULL,
    notification_status VARCHAR(20) NOT NULL DEFAULT 'UNREAD'
        CHECK (notification_status IN ('UNREAD', 'READ', 'ARCHIVED', 'DELETED')),
    notification_related_url VARCHAR(255),
    notification_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (notification_user_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE INDEX idx_notifications_user ON notifications(notification_user_id) WHERE notification_deleted_at IS NULL;
CREATE INDEX idx_notifications_status ON notifications(notification_status) WHERE notification_deleted_at IS NULL;
CREATE INDEX idx_notifications_type ON notifications(notification_type) WHERE notification_deleted_at IS NULL;
CREATE INDEX idx_notifications_created ON notifications(notification_created_at) WHERE notification_deleted_at IS NULL;
CREATE INDEX idx_notifications_user_id ON notifications(notification_user_id);

CREATE TABLE reviews (
    review_id SERIAL PRIMARY KEY,
    product_detail_id INTEGER NOT NULL,
    customer_id INTEGER NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
        CHECK (review_status IN ('APPROVED', 'PENDING', 'REJECTED', 'DELETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE RESTRICT,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_reviews_product_customer_active ON reviews(product_detail_id, customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_product ON reviews(product_detail_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_customer ON reviews(customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_rating ON reviews(rating) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_status ON reviews(review_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_created ON reviews(created_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_product_detail_id ON reviews(product_detail_id);
CREATE INDEX idx_reviews_rating_idx ON reviews(rating);

CREATE TABLE shipments (
    shipment_id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL,
    shipping_address_id INTEGER NOT NULL,
    shipment_date TIMESTAMP,
    delivery_date TIMESTAMP,
    shipment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (shipment_status IN ('PENDING', 'SHIPPING', 'DELIVERED', 'FAILED', 'CANCELLED', 'DELETED')),
    tracking_number VARCHAR(50),
    carrier_name VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE RESTRICT,
    FOREIGN KEY (shipping_address_id) REFERENCES shipping_addresses(shipping_address_id) ON DELETE RESTRICT
);

CREATE INDEX idx_shipments_order ON shipments(order_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_shipments_tracking ON shipments(tracking_number) WHERE deleted_at IS NULL;
CREATE INDEX idx_shipments_status ON shipments(shipment_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_shipments_order_id ON shipments(order_id);
CREATE INDEX idx_shipments_tracking_idx ON shipments(tracking_number);

CREATE TABLE wishlists (
    wishlist_id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    wishlist_name VARCHAR(100) NOT NULL DEFAULT 'Default',
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    wishlist_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (wishlist_status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT
);

CREATE INDEX idx_wishlists_customer ON wishlists(customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_wishlists_customer_id ON wishlists(customer_id);

CREATE TABLE wishlist_items (
    wishlist_item_id SERIAL PRIMARY KEY,
    wishlist_item_wishlist_id INTEGER NOT NULL,
    wishlist_item_product_detail_id INTEGER NOT NULL,
    wishlist_item_added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    wishlist_item_notes TEXT,
    wishlist_item_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (wishlist_item_status IN ('ACTIVE', 'DELETED')),
    wishlist_item_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    wishlist_item_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    wishlist_item_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (wishlist_item_wishlist_id) REFERENCES wishlists(wishlist_id) ON DELETE CASCADE,
    FOREIGN KEY (wishlist_item_product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_wishlist_items_active ON wishlist_items(wishlist_item_wishlist_id, wishlist_item_product_detail_id) WHERE wishlist_item_deleted_at IS NULL;
CREATE INDEX idx_wishlist_items_wishlist ON wishlist_items(wishlist_item_wishlist_id) WHERE wishlist_item_deleted_at IS NULL;
CREATE INDEX idx_wishlist_items_product ON wishlist_items(wishlist_item_product_detail_id) WHERE wishlist_item_deleted_at IS NULL;
CREATE INDEX idx_wishlist_items_wishlist_id ON wishlist_items(wishlist_item_wishlist_id);

CREATE TABLE return_orders (
    return_id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL,
    customer_id INTEGER,
    return_reason TEXT,
    return_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (return_status IN ('PENDING', 'APPROVED', 'REJECTED', 'REFUNDED', 'CANCELLED', 'DELETED')),
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approval_date TIMESTAMP,
    return_refund_amount DECIMAL(15,2) CHECK (return_refund_amount IS NULL OR return_refund_amount >= 0),
    refund_method_id INTEGER,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE RESTRICT,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL,
    FOREIGN KEY (refund_method_id) REFERENCES payment_methods(payment_method_id) ON DELETE SET NULL
);

CREATE INDEX idx_return_orders_order ON return_orders(order_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_return_orders_customer ON return_orders(customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_return_orders_status ON return_orders(return_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_return_orders_order_id ON return_orders(order_id);

CREATE TABLE return_items (
    return_item_id SERIAL PRIMARY KEY,
    return_id INTEGER NOT NULL,                          
    order_detail_id INTEGER NOT NULL,                   
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'REFUNDED', 'CANCELLED', 'DELETED')),
    return_item_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    return_item_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    return_item_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (return_id) REFERENCES return_orders(return_id) ON DELETE CASCADE,              
    FOREIGN KEY (order_detail_id) REFERENCES order_details(order_detail_id) ON DELETE RESTRICT 
);

CREATE INDEX idx_return_items_return ON return_items(return_id) WHERE return_item_deleted_at IS NULL;
CREATE INDEX idx_return_items_order_detail ON return_items(order_detail_id) WHERE return_item_deleted_at IS NULL;
CREATE INDEX idx_return_items_return_id ON return_items(return_id);

CREATE TABLE statistical_reports (
    report_id SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    employee_id INTEGER,                             
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    file_path VARCHAR(255),
    note TEXT,
    report_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    report_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    report_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE SET NULL  
);

CREATE INDEX idx_statistical_reports_type ON statistical_reports(report_type) WHERE report_deleted_at IS NULL;
CREATE INDEX idx_statistical_reports_status ON statistical_reports(status) WHERE report_deleted_at IS NULL;
CREATE INDEX idx_statistical_reports_created ON statistical_reports(created_date) WHERE report_deleted_at IS NULL;
CREATE INDEX idx_statistical_reports_employee ON statistical_reports(employee_id) WHERE report_deleted_at IS NULL;

CREATE TABLE report_details (
    report_detail_id SERIAL PRIMARY KEY,
    report_id INTEGER NOT NULL,                          
    product_id INTEGER,                                 
    quantity_sold INTEGER NOT NULL DEFAULT 0 CHECK (quantity_sold >= 0),
    revenue DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (revenue >= 0),
    cost DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (cost >= 0),
    profit DECIMAL(15,2) NOT NULL DEFAULT 0,
    report_detail_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    report_detail_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    report_detail_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (report_id) REFERENCES statistical_reports(report_id) ON DELETE CASCADE,    
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE SET NULL              
);

CREATE INDEX idx_report_details_report ON report_details(report_id) WHERE report_detail_deleted_at IS NULL;
CREATE INDEX idx_report_details_product ON report_details(product_id) WHERE report_detail_deleted_at IS NULL;
-- =====================================================
-- TRIGGERS - ĐÃ SỬA ĐỂ ĐÚNG TÊN CỘT
-- =====================================================

CREATE TRIGGER update_users_modtime BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_roles_modtime BEFORE UPDATE ON roles FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_customers_modtime BEFORE UPDATE ON customers FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_employees_modtime BEFORE UPDATE ON employees FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_categories_modtime BEFORE UPDATE ON categories FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_products_modtime BEFORE UPDATE ON products FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_sizes_modtime BEFORE UPDATE ON sizes FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_colors_modtime BEFORE UPDATE ON colors FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_product_details_modtime BEFORE UPDATE ON product_details FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_product_images_modtime BEFORE UPDATE ON product_images FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_promotions_modtime BEFORE UPDATE ON promotions FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_vouchers_modtime BEFORE UPDATE ON vouchers FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_carts_modtime BEFORE UPDATE ON carts FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_cart_items_modtime BEFORE UPDATE ON cart_items FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_shipping_addresses_modtime BEFORE UPDATE ON shipping_addresses FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_orders_modtime BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_payment_methods_modtime BEFORE UPDATE ON payment_methods FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_payments_modtime BEFORE UPDATE ON payments FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_payment_transactions_modtime BEFORE UPDATE ON payment_transactions FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_chats_modtime BEFORE UPDATE ON chats FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_messages_modtime BEFORE UPDATE ON messages FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_notifications_modtime BEFORE UPDATE ON notifications FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_reviews_modtime BEFORE UPDATE ON reviews FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_shipments_modtime BEFORE UPDATE ON shipments FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_wishlists_modtime BEFORE UPDATE ON wishlists FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_wishlist_items_modtime BEFORE UPDATE ON wishlist_items FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_return_orders_modtime BEFORE UPDATE ON return_orders FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_return_items_modtime BEFORE UPDATE ON return_items FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_statistical_reports_modtime BEFORE UPDATE ON statistical_reports FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_report_details_modtime BEFORE UPDATE ON report_details FOR EACH ROW EXECUTE FUNCTION update_modified_column();

-- =====================================================
-- FUNCTIONS & TRIGGERS CHO VOUCHER, POS, FLASH SALE
-- =====================================================

CREATE OR REPLACE FUNCTION check_voucher_usage_limit()
RETURNS TRIGGER AS $$
DECLARE
    v_max_usage INTEGER;
    v_current_usage INTEGER;
    v_status VARCHAR(20);
BEGIN
    SELECT voucher_max_usage, voucher_used_count, voucher_status
    INTO v_max_usage, v_current_usage, v_status
    FROM vouchers
    WHERE voucher_id = NEW.voucher_id;

    IF v_status <> 'ACTIVE' THEN
        RAISE EXCEPTION 'Voucher is not active';
    END IF;

    IF v_max_usage IS NOT NULL AND v_current_usage >= v_max_usage THEN
        RAISE EXCEPTION 'Voucher has reached its usage limit';
    END IF;

    UPDATE vouchers
    SET voucher_used_count = voucher_used_count + 1
    WHERE voucher_id = NEW.voucher_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_voucher_usage_before_insert
BEFORE INSERT ON voucher_usage
FOR EACH ROW
EXECUTE FUNCTION check_voucher_usage_limit();

-- =====================================================
-- INSERT DỮ LIỆU MẪU
-- =====================================================

INSERT INTO roles(role_name) VALUES
('ADMIN'),
('STAFF'),
('CUSTOMER');

-- =====================================================
-- BẢNG POS (Point of Sale)
-- =====================================================

CREATE TABLE cash_registers (
    cash_register_id SERIAL PRIMARY KEY,
    cash_register_name VARCHAR(100) NOT NULL,
    cash_register_location VARCHAR(255),
    cash_register_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (cash_register_status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DELETED')),
    cash_register_opened_at TIMESTAMP,
    cash_register_closed_at TIMESTAMP,
    cash_register_opening_balance DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (cash_register_opening_balance >= 0),
    cash_register_closing_balance DECIMAL(15,2),
    cash_register_expected_balance DECIMAL(15,2),
    cash_register_difference DECIMAL(15,2),
    cash_register_opened_by INTEGER,
    cash_register_closed_by INTEGER,
    cash_register_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cash_register_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cash_register_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (cash_register_opened_by) REFERENCES employees(employee_id) ON DELETE SET NULL,
    FOREIGN KEY (cash_register_closed_by) REFERENCES employees(employee_id) ON DELETE SET NULL
);

CREATE INDEX idx_cash_registers_status ON cash_registers(cash_register_status) WHERE cash_register_deleted_at IS NULL;
CREATE INDEX idx_cash_registers_opened_by ON cash_registers(cash_register_opened_by) WHERE cash_register_deleted_at IS NULL;
CREATE INDEX idx_cash_registers_date ON cash_registers(cash_register_opened_at) WHERE cash_register_deleted_at IS NULL;

CREATE TABLE pos_sessions (
    pos_session_id SERIAL PRIMARY KEY,
    pos_session_cash_register_id INTEGER NOT NULL,
    pos_session_employee_id INTEGER NOT NULL,
    pos_session_start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_session_end_time TIMESTAMP,
    pos_session_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (pos_session_status IN ('ACTIVE', 'SUSPENDED', 'CLOSED', 'DELETED')),
    pos_session_start_balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    pos_session_end_balance DECIMAL(15,2),
    pos_session_cash_sales DECIMAL(15,2) NOT NULL DEFAULT 0,
    pos_session_card_sales DECIMAL(15,2) NOT NULL DEFAULT 0,
    pos_session_transfer_sales DECIMAL(15,2) NOT NULL DEFAULT 0,
    pos_session_total_sales DECIMAL(15,2) GENERATED ALWAYS AS 
        (pos_session_cash_sales + pos_session_card_sales + pos_session_transfer_sales) STORED,
    pos_session_notes TEXT,
    pos_session_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_session_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_session_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (pos_session_cash_register_id) REFERENCES cash_registers(cash_register_id) ON DELETE RESTRICT,
    FOREIGN KEY (pos_session_employee_id) REFERENCES employees(employee_id) ON DELETE RESTRICT
);

CREATE INDEX idx_pos_sessions_register ON pos_sessions(pos_session_cash_register_id) WHERE pos_session_deleted_at IS NULL;
CREATE INDEX idx_pos_sessions_employee ON pos_sessions(pos_session_employee_id) WHERE pos_session_deleted_at IS NULL;
CREATE INDEX idx_pos_sessions_status ON pos_sessions(pos_session_status) WHERE pos_session_deleted_at IS NULL;
CREATE INDEX idx_pos_sessions_start_time ON pos_sessions(pos_session_start_time) WHERE pos_session_deleted_at IS NULL;

CREATE TABLE pos_orders (
    pos_order_id SERIAL PRIMARY KEY,
    pos_order_session_id INTEGER NOT NULL,
    pos_order_customer_id INTEGER,
    pos_order_invoice_number VARCHAR(50) UNIQUE NOT NULL,
    pos_order_type VARCHAR(20) NOT NULL DEFAULT 'AT_COUNTER'
        CHECK (pos_order_type IN ('AT_COUNTER', 'TAKEAWAY', 'DELIVERY')),
    pos_order_subtotal DECIMAL(15,2) NOT NULL CHECK (pos_order_subtotal >= 0),
    pos_order_discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (pos_order_discount_amount >= 0),
    pos_order_tax_amount DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (pos_order_tax_amount >= 0),
    pos_order_total_amount DECIMAL(15,2) GENERATED ALWAYS AS 
        (pos_order_subtotal - pos_order_discount_amount + pos_order_tax_amount) STORED,
    pos_order_paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (pos_order_paid_amount >= 0),
    pos_order_change_amount DECIMAL(15,2),  -- ✅ Bỏ GENERATED, tính bằng trigger hoặc application
    pos_order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (pos_order_status IN ('PENDING', 'PENDING_PAYMENT', 'COMPLETED', 'CANCELLED', 'REFUNDED', 'DELETED')),
    pos_order_payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
        CHECK (pos_order_payment_status IN ('UNPAID', 'PAID', 'PARTIAL', 'REFUNDED', 'DELETED')),
    pos_order_customer_name VARCHAR(100),
    pos_order_customer_phone VARCHAR(20),
    pos_order_notes TEXT,
    pos_order_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_order_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_order_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (pos_order_session_id) REFERENCES pos_sessions(pos_session_id) ON DELETE RESTRICT,
    FOREIGN KEY (pos_order_customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL
);

-- Tạo trigger để tự động tính pos_order_change_amount
CREATE OR REPLACE FUNCTION calculate_pos_order_change()
RETURNS TRIGGER AS $$
BEGIN
    NEW.pos_order_change_amount = NEW.pos_order_paid_amount - NEW.pos_order_total_amount;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_calculate_pos_order_change
BEFORE INSERT OR UPDATE ON pos_orders
FOR EACH ROW
EXECUTE FUNCTION calculate_pos_order_change();

CREATE INDEX idx_pos_orders_session ON pos_orders(pos_order_session_id) WHERE pos_order_deleted_at IS NULL;
CREATE INDEX idx_pos_orders_customer ON pos_orders(pos_order_customer_id) WHERE pos_order_deleted_at IS NULL;
CREATE INDEX idx_pos_orders_status ON pos_orders(pos_order_status) WHERE pos_order_deleted_at IS NULL;
CREATE INDEX idx_pos_orders_invoice ON pos_orders(pos_order_invoice_number) WHERE pos_order_deleted_at IS NULL;
CREATE INDEX idx_pos_orders_created_at ON pos_orders(pos_order_created_at) WHERE pos_order_deleted_at IS NULL;

CREATE TABLE pos_order_items (
    pos_order_item_id SERIAL PRIMARY KEY,
    pos_order_item_order_id INTEGER NOT NULL,
    pos_order_item_product_detail_id INTEGER NOT NULL,
    pos_order_item_quantity INTEGER NOT NULL CHECK (pos_order_item_quantity > 0),
    pos_order_item_unit_price DECIMAL(15,2) NOT NULL CHECK (pos_order_item_unit_price >= 0),
    pos_order_item_discount DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (pos_order_item_discount >= 0),
    pos_order_item_total DECIMAL(15,2) GENERATED ALWAYS AS 
        ((pos_order_item_unit_price - pos_order_item_discount) * pos_order_item_quantity) STORED,
    pos_order_item_notes TEXT,
    pos_order_item_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_order_item_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_order_item_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (pos_order_item_order_id) REFERENCES pos_orders(pos_order_id) ON DELETE CASCADE,
    FOREIGN KEY (pos_order_item_product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE RESTRICT
);

CREATE INDEX idx_pos_order_items_order ON pos_order_items(pos_order_item_order_id) WHERE pos_order_item_deleted_at IS NULL;
CREATE INDEX idx_pos_order_items_product ON pos_order_items(pos_order_item_product_detail_id) WHERE pos_order_item_deleted_at IS NULL;

CREATE TABLE pos_payments (
    pos_payment_id SERIAL PRIMARY KEY,
    pos_payment_order_id INTEGER NOT NULL,
    pos_payment_method_id INTEGER NOT NULL,
    pos_payment_amount DECIMAL(15,2) NOT NULL CHECK (pos_payment_amount > 0),
    pos_payment_reference_number VARCHAR(100),
    pos_payment_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
        CHECK (pos_payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED', 'DELETED')),
    pos_payment_processed_by INTEGER,
    pos_payment_processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_payment_notes TEXT,
    pos_payment_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_payment_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_payment_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (pos_payment_order_id) REFERENCES pos_orders(pos_order_id) ON DELETE CASCADE,
    FOREIGN KEY (pos_payment_method_id) REFERENCES payment_methods(payment_method_id) ON DELETE RESTRICT,
    FOREIGN KEY (pos_payment_processed_by) REFERENCES employees(employee_id) ON DELETE SET NULL
);

CREATE INDEX idx_pos_payments_order ON pos_payments(pos_payment_order_id) WHERE pos_payment_deleted_at IS NULL;
CREATE INDEX idx_pos_payments_method ON pos_payments(pos_payment_method_id) WHERE pos_payment_deleted_at IS NULL;
CREATE INDEX idx_pos_payments_status ON pos_payments(pos_payment_status) WHERE pos_payment_deleted_at IS NULL;
CREATE INDEX idx_pos_payments_processed_at ON pos_payments(pos_payment_processed_at) WHERE pos_payment_deleted_at IS NULL;

CREATE TABLE pos_holds (
    pos_hold_id SERIAL PRIMARY KEY,
    pos_hold_session_id INTEGER NOT NULL,
    pos_hold_customer_id INTEGER,
    pos_hold_reference_code VARCHAR(50) UNIQUE NOT NULL,
    pos_hold_items_data JSONB,
    pos_hold_subtotal DECIMAL(15,2) NOT NULL DEFAULT 0,
    pos_hold_discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    pos_hold_total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    pos_hold_notes TEXT,
    pos_hold_expiry_time TIMESTAMP NOT NULL,
    pos_hold_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (pos_hold_status IN ('ACTIVE', 'CONVERTED', 'EXPIRED', 'CANCELLED', 'DELETED')),
    pos_hold_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_hold_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_hold_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (pos_hold_session_id) REFERENCES pos_sessions(pos_session_id) ON DELETE RESTRICT,
    FOREIGN KEY (pos_hold_customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL
);

CREATE INDEX idx_pos_holds_session ON pos_holds(pos_hold_session_id) WHERE pos_hold_deleted_at IS NULL;
CREATE INDEX idx_pos_holds_customer ON pos_holds(pos_hold_customer_id) WHERE pos_hold_deleted_at IS NULL;
CREATE INDEX idx_pos_holds_reference ON pos_holds(pos_hold_reference_code) WHERE pos_hold_deleted_at IS NULL;
CREATE INDEX idx_pos_holds_status ON pos_holds(pos_hold_status) WHERE pos_hold_deleted_at IS NULL;
CREATE INDEX idx_pos_holds_expiry ON pos_holds(pos_hold_expiry_time) WHERE pos_hold_status = 'ACTIVE' AND pos_hold_deleted_at IS NULL;

CREATE TABLE daily_reports (
    daily_report_id SERIAL PRIMARY KEY,
    daily_report_date DATE NOT NULL,
    daily_report_cash_register_id INTEGER NOT NULL,
    daily_report_opening_balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    daily_report_closing_balance DECIMAL(15,2),
    daily_report_cash_sales DECIMAL(15,2) NOT NULL DEFAULT 0,
    daily_report_card_sales DECIMAL(15,2) NOT NULL DEFAULT 0,
    daily_report_transfer_sales DECIMAL(15,2) NOT NULL DEFAULT 0,
    daily_report_total_sales DECIMAL(15,2) GENERATED ALWAYS AS 
        (daily_report_cash_sales + daily_report_card_sales + daily_report_transfer_sales) STORED,
    daily_report_total_orders INTEGER NOT NULL DEFAULT 0,
    daily_report_total_customers INTEGER NOT NULL DEFAULT 0,
    daily_report_avg_order_value DECIMAL(15,2),
    daily_report_created_by INTEGER,
    daily_report_notes TEXT,
    daily_report_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    daily_report_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    daily_report_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (daily_report_cash_register_id) REFERENCES cash_registers(cash_register_id) ON DELETE RESTRICT,
    FOREIGN KEY (daily_report_created_by) REFERENCES employees(employee_id) ON DELETE SET NULL,
    UNIQUE(daily_report_date, daily_report_cash_register_id)
);

CREATE INDEX idx_daily_reports_date ON daily_reports(daily_report_date) WHERE daily_report_deleted_at IS NULL;
CREATE INDEX idx_daily_reports_register ON daily_reports(daily_report_cash_register_id) WHERE daily_report_deleted_at IS NULL;

-- =====================================================
-- POS TRIGGERS
-- =====================================================

CREATE TRIGGER update_cash_registers_modtime BEFORE UPDATE ON cash_registers FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_pos_sessions_modtime BEFORE UPDATE ON pos_sessions FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_pos_orders_modtime BEFORE UPDATE ON pos_orders FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_pos_order_items_modtime BEFORE UPDATE ON pos_order_items FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_pos_payments_modtime BEFORE UPDATE ON pos_payments FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_pos_holds_modtime BEFORE UPDATE ON pos_holds FOR EACH ROW EXECUTE FUNCTION update_modified_column();
CREATE TRIGGER update_daily_reports_modtime BEFORE UPDATE ON daily_reports FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE OR REPLACE FUNCTION update_product_stock_on_pos_sale()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE product_details
    SET product_detail_quantity = product_detail_quantity - NEW.pos_order_item_quantity,
        product_detail_updated_at = NOW()
    WHERE product_detail_id = NEW.pos_order_item_product_detail_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_stock_on_pos_sale
AFTER INSERT ON pos_order_items
FOR EACH ROW
EXECUTE FUNCTION update_product_stock_on_pos_sale();

CREATE OR REPLACE FUNCTION create_daily_report_on_session_close()
RETURNS TRIGGER AS $$
DECLARE
    v_total_orders INTEGER;
    v_total_sales DECIMAL;
    v_total_customers INTEGER;
BEGIN
    IF NEW.pos_session_status = 'CLOSED' AND OLD.pos_session_status != 'CLOSED' THEN
        SELECT COUNT(*), COALESCE(SUM(pos_order_total_amount), 0)
        INTO v_total_orders, v_total_sales
        FROM pos_orders
        WHERE pos_order_session_id = NEW.pos_session_id
        AND pos_order_status = 'COMPLETED'
        AND pos_order_deleted_at IS NULL;
        
        SELECT COUNT(DISTINCT pos_order_customer_id)
        INTO v_total_customers
        FROM pos_orders
        WHERE pos_order_session_id = NEW.pos_session_id
        AND pos_order_status = 'COMPLETED'
        AND pos_order_customer_id IS NOT NULL
        AND pos_order_deleted_at IS NULL;
        
        IF v_total_sales IS NULL THEN v_total_sales := 0; END IF;
        IF v_total_orders IS NULL THEN v_total_orders := 0; END IF;
        IF v_total_customers IS NULL THEN v_total_customers := 0; END IF;
        
        NEW.pos_session_total_sales = v_total_sales;
        NEW.pos_session_end_balance = COALESCE(NEW.pos_session_start_balance, 0) + v_total_sales;
        NEW.pos_session_end_time = NOW();
        
        INSERT INTO daily_reports (
            daily_report_date,
            daily_report_cash_register_id,
            daily_report_opening_balance,
            daily_report_closing_balance,
            daily_report_cash_sales,
            daily_report_total_orders,
            daily_report_total_customers,
            daily_report_avg_order_value,
            daily_report_created_by
        ) VALUES (
            CURRENT_DATE,
            NEW.pos_session_cash_register_id,
            COALESCE(NEW.pos_session_start_balance, 0),
            COALESCE(NEW.pos_session_end_balance, 0),
            v_total_sales,
            v_total_orders,
            v_total_customers,
            CASE WHEN v_total_orders > 0 THEN v_total_sales / v_total_orders ELSE 0 END,
            NEW.pos_session_employee_id
        )
        ON CONFLICT (daily_report_date, daily_report_cash_register_id) 
        DO UPDATE SET
            daily_report_closing_balance = EXCLUDED.daily_report_closing_balance,
            daily_report_cash_sales = COALESCE(daily_reports.daily_report_cash_sales, 0) + EXCLUDED.daily_report_cash_sales,
            daily_report_total_orders = COALESCE(daily_reports.daily_report_total_orders, 0) + EXCLUDED.daily_report_total_orders,
            daily_report_total_customers = COALESCE(daily_reports.daily_report_total_customers, 0) + EXCLUDED.daily_report_total_customers,
            daily_report_avg_order_value = (COALESCE(daily_reports.daily_report_total_sales, 0) + EXCLUDED.daily_report_cash_sales) / 
                                          NULLIF((COALESCE(daily_reports.daily_report_total_orders, 0) + EXCLUDED.daily_report_total_orders), 0),
            daily_report_updated_at = NOW();
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER create_report_on_session_close
BEFORE UPDATE ON pos_sessions
FOR EACH ROW
EXECUTE FUNCTION create_daily_report_on_session_close();

-- =====================================================
-- BẢNG POS CASH TRANSACTIONS, REFUNDS, RECEIPT LOGS
-- =====================================================

CREATE TABLE pos_cash_transactions (
    cash_transaction_id SERIAL PRIMARY KEY,
    cash_transaction_session_id INTEGER NOT NULL,
    cash_transaction_type VARCHAR(20) NOT NULL CHECK (cash_transaction_type IN ('WITHDRAW', 'DEPOSIT')),
    cash_transaction_amount DECIMAL(15,2) NOT NULL CHECK (cash_transaction_amount > 0),
    cash_transaction_reason TEXT NOT NULL,
    cash_transaction_employee_id INTEGER NOT NULL,
    cash_transaction_before_balance DECIMAL(15,2) NOT NULL,
    cash_transaction_after_balance DECIMAL(15,2) NOT NULL,
    cash_transaction_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cash_transaction_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (cash_transaction_session_id) REFERENCES pos_sessions(pos_session_id) ON DELETE RESTRICT,
    FOREIGN KEY (cash_transaction_employee_id) REFERENCES employees(employee_id) ON DELETE RESTRICT
);

CREATE INDEX idx_cash_transactions_session ON pos_cash_transactions(cash_transaction_session_id) WHERE cash_transaction_deleted_at IS NULL;
CREATE INDEX idx_cash_transactions_type ON pos_cash_transactions(cash_transaction_type) WHERE cash_transaction_deleted_at IS NULL;

CREATE TABLE pos_refunds (
    pos_refund_id SERIAL PRIMARY KEY,
    pos_refund_order_id INTEGER NOT NULL,
    pos_refund_amount DECIMAL(15,2) NOT NULL CHECK (pos_refund_amount > 0),
    pos_refund_reason TEXT NOT NULL,
    pos_refund_method_id INTEGER NOT NULL,
    pos_refund_employee_id INTEGER NOT NULL,
    pos_refund_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (pos_refund_status IN ('PENDING', 'COMPLETED', 'REJECTED', 'CANCELLED')),
    pos_refund_approved_by INTEGER,
    pos_refund_approved_at TIMESTAMP,
    pos_refund_notes TEXT,
    pos_refund_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_refund_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pos_refund_deleted_at TIMESTAMP NULL,
    FOREIGN KEY (pos_refund_order_id) REFERENCES pos_orders(pos_order_id) ON DELETE RESTRICT,
    FOREIGN KEY (pos_refund_method_id) REFERENCES payment_methods(payment_method_id) ON DELETE RESTRICT,
    FOREIGN KEY (pos_refund_employee_id) REFERENCES employees(employee_id) ON DELETE RESTRICT,
    FOREIGN KEY (pos_refund_approved_by) REFERENCES employees(employee_id) ON DELETE SET NULL
);

CREATE INDEX idx_pos_refunds_order ON pos_refunds(pos_refund_order_id) WHERE pos_refund_deleted_at IS NULL;
CREATE INDEX idx_pos_refunds_status ON pos_refunds(pos_refund_status) WHERE pos_refund_deleted_at IS NULL;

CREATE TABLE pos_receipt_logs (
    receipt_log_id SERIAL PRIMARY KEY,
    receipt_log_order_id INTEGER NOT NULL,
    receipt_log_printed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    receipt_log_printed_by INTEGER NOT NULL,
    receipt_log_copies INTEGER NOT NULL DEFAULT 1 CHECK (receipt_log_copies > 0),
    receipt_log_printer_name VARCHAR(100),
    receipt_log_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'
        CHECK (receipt_log_status IN ('SUCCESS', 'FAILED', 'RETRY')),
    receipt_log_error_message TEXT,
    FOREIGN KEY (receipt_log_order_id) REFERENCES pos_orders(pos_order_id) ON DELETE CASCADE,
    FOREIGN KEY (receipt_log_printed_by) REFERENCES employees(employee_id) ON DELETE RESTRICT
);

CREATE INDEX idx_receipt_logs_order ON pos_receipt_logs(receipt_log_order_id);
CREATE INDEX idx_receipt_logs_printed_at ON pos_receipt_logs(receipt_log_printed_at);

-- =====================================================
-- BẢNG PRODUCT STOCK ALERTS & PRICE HISTORY
-- =====================================================

CREATE TABLE product_stock_alerts (
    stock_alert_id SERIAL PRIMARY KEY,
    stock_alert_product_detail_id INTEGER NOT NULL,
    stock_alert_threshold INTEGER NOT NULL DEFAULT 10,
    stock_alert_current_stock INTEGER NOT NULL,
    stock_alert_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (stock_alert_status IN ('PENDING', 'RESOLVED', 'IGNORED')),
    stock_alert_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stock_alert_resolved_at TIMESTAMP,
    stock_alert_resolved_by INTEGER,
    FOREIGN KEY (stock_alert_product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE CASCADE,
    FOREIGN KEY (stock_alert_resolved_by) REFERENCES employees(employee_id) ON DELETE SET NULL
);

CREATE INDEX idx_stock_alerts_product ON product_stock_alerts(stock_alert_product_detail_id) WHERE stock_alert_status = 'PENDING';
CREATE INDEX idx_stock_alerts_status ON product_stock_alerts(stock_alert_status);

CREATE OR REPLACE FUNCTION check_stock_threshold_alert()
RETURNS TRIGGER AS $$
DECLARE
    v_threshold INTEGER;
BEGIN
    SELECT COALESCE(stock_alert_threshold, 10) INTO v_threshold
    FROM product_stock_alerts
    WHERE stock_alert_product_detail_id = NEW.product_detail_id
    AND stock_alert_status = 'PENDING'
    ORDER BY stock_alert_created_at DESC
    LIMIT 1;
    
    IF v_threshold IS NULL THEN
        v_threshold := 10;
    END IF;
    IF NEW.product_detail_quantity <= v_threshold AND OLD.product_detail_quantity > v_threshold THEN
        INSERT INTO product_stock_alerts (stock_alert_product_detail_id, stock_alert_threshold, stock_alert_current_stock)
        VALUES (NEW.product_detail_id, v_threshold, NEW.product_detail_quantity);
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_stock_threshold_alert
AFTER UPDATE ON product_details
FOR EACH ROW
WHEN (NEW.product_detail_quantity <= 10 AND OLD.product_detail_quantity > 10)
EXECUTE FUNCTION check_stock_threshold_alert();

CREATE TABLE product_price_history (
    price_history_id SERIAL PRIMARY KEY,
    price_history_product_detail_id INTEGER NOT NULL,
    price_history_old_price DECIMAL(15,2) NOT NULL,
    price_history_new_price DECIMAL(15,2) NOT NULL,
    price_history_changed_by INTEGER,
    price_history_reason TEXT,
    price_history_changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (price_history_product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE CASCADE,
    FOREIGN KEY (price_history_changed_by) REFERENCES employees(employee_id) ON DELETE SET NULL
);

CREATE INDEX idx_price_history_product ON product_price_history(price_history_product_detail_id);
CREATE INDEX idx_price_history_changed_at ON product_price_history(price_history_changed_at);

CREATE OR REPLACE FUNCTION log_price_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.product_detail_price != NEW.product_detail_price THEN
        INSERT INTO product_price_history (
            price_history_product_detail_id,
            price_history_old_price,
            price_history_new_price
        ) VALUES (
            NEW.product_detail_id,
            OLD.product_detail_price,
            NEW.product_detail_price
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER log_price_change
AFTER UPDATE ON product_details
FOR EACH ROW
WHEN (OLD.product_detail_price IS DISTINCT FROM NEW.product_detail_price)
EXECUTE FUNCTION log_price_change();

-- =====================================================
-- BẢNG FLASH SALE
-- =====================================================

CREATE TABLE flash_sales (
    flash_sale_id SERIAL PRIMARY KEY,
    flash_sale_name VARCHAR(100) NOT NULL,
    flash_sale_product_detail_id INTEGER NOT NULL,
    flash_sale_discount_percent DECIMAL(5,2) NOT NULL CHECK (flash_sale_discount_percent BETWEEN 0 AND 100),
    flash_sale_start_time TIMESTAMP NOT NULL,
    flash_sale_end_time TIMESTAMP NOT NULL,
    flash_sale_quantity_limit INTEGER CHECK (flash_sale_quantity_limit > 0),
    flash_sale_sold_quantity INTEGER NOT NULL DEFAULT 0,
    flash_sale_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (flash_sale_status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    flash_sale_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    flash_sale_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (flash_sale_product_detail_id) REFERENCES product_details(product_detail_id) ON DELETE CASCADE,
    CHECK (flash_sale_end_time > flash_sale_start_time)
);

CREATE INDEX idx_flash_sales_active ON flash_sales(flash_sale_product_detail_id, flash_sale_start_time, flash_sale_end_time)
    WHERE flash_sale_status = 'ACTIVE';

CREATE OR REPLACE FUNCTION check_stock_before_pos_sale()
RETURNS TRIGGER AS $$
DECLARE
    v_current_stock INTEGER;
    v_product_name VARCHAR(100);
BEGIN
    SELECT product_detail_quantity, p.product_name
    INTO v_current_stock, v_product_name
    FROM product_details pd
    JOIN products p ON pd.product_detail_product_id = p.product_id
    WHERE pd.product_detail_id = NEW.pos_order_item_product_detail_id;
    
    IF v_current_stock < NEW.pos_order_item_quantity THEN
        RAISE EXCEPTION 'Sản phẩm % không đủ tồn kho. Còn: %, Yêu cầu: %', 
            v_product_name, v_current_stock, NEW.pos_order_item_quantity;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_stock_before_pos_sale
BEFORE INSERT ON pos_order_items
FOR EACH ROW
EXECUTE FUNCTION check_stock_before_pos_sale();

CREATE OR REPLACE FUNCTION apply_flash_sale_to_pos_item()
RETURNS TRIGGER AS $$
DECLARE
    v_flash_sale_discount DECIMAL(5,2);
    v_max_quantity INTEGER;
BEGIN
    SELECT flash_sale_discount_percent, flash_sale_quantity_limit - flash_sale_sold_quantity
    INTO v_flash_sale_discount, v_max_quantity
    FROM flash_sales
    WHERE flash_sale_product_detail_id = NEW.pos_order_item_product_detail_id
    AND flash_sale_status = 'ACTIVE'
    AND NOW() BETWEEN flash_sale_start_time AND flash_sale_end_time
    LIMIT 1;
    
    IF v_flash_sale_discount IS NOT NULL THEN
        IF NEW.pos_order_item_quantity > v_max_quantity THEN
            RAISE EXCEPTION 'Số lượng mua vượt quá giới hạn flash sale. Tối đa: %', v_max_quantity;
        END IF;
        
        IF v_flash_sale_discount > (NEW.pos_order_item_discount / NEW.pos_order_item_unit_price * 100) THEN
            NEW.pos_order_item_discount := NEW.pos_order_item_unit_price * v_flash_sale_discount / 100;
        END IF;
        
        UPDATE flash_sales
        SET flash_sale_sold_quantity = flash_sale_sold_quantity + NEW.pos_order_item_quantity
        WHERE flash_sale_product_detail_id = NEW.pos_order_item_product_detail_id
        AND flash_sale_status = 'ACTIVE';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER apply_flash_sale_to_pos_item
BEFORE INSERT ON pos_order_items
FOR EACH ROW
EXECUTE FUNCTION apply_flash_sale_to_pos_item();

-- =====================================================
-- BẢNG INVOICE SEQUENCES
-- =====================================================

CREATE TABLE invoice_sequences (
    sequence_id SERIAL PRIMARY KEY,
    sequence_prefix VARCHAR(10) NOT NULL,
    sequence_current_number INTEGER NOT NULL DEFAULT 1,
    sequence_year INTEGER NOT NULL,
    sequence_month INTEGER NOT NULL,
    UNIQUE(sequence_prefix, sequence_year, sequence_month)
);

CREATE OR REPLACE FUNCTION generate_invoice_number(p_prefix VARCHAR)
RETURNS VARCHAR AS $$
DECLARE
    v_year INTEGER := EXTRACT(YEAR FROM CURRENT_DATE);
    v_month INTEGER := EXTRACT(MONTH FROM CURRENT_DATE);
    v_current_num INTEGER;
    v_invoice_number VARCHAR;
BEGIN
    INSERT INTO invoice_sequences (sequence_prefix, sequence_year, sequence_month, sequence_current_number)
    VALUES (p_prefix, v_year, v_month, 1)
    ON CONFLICT (sequence_prefix, sequence_year, sequence_month)
    DO UPDATE SET sequence_current_number = invoice_sequences.sequence_current_number + 1
    RETURNING sequence_current_number INTO v_current_num;
    
    v_invoice_number := p_prefix || TO_CHAR(CURRENT_DATE, 'YYYYMM') || LPAD(v_current_num::TEXT, 6, '0');
    RETURN v_invoice_number;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- VIEWS
-- =====================================================

CREATE OR REPLACE VIEW view_pos_hourly_revenue AS
SELECT 
    DATE(pos_order_created_at) AS sale_date,
    EXTRACT(HOUR FROM pos_order_created_at) AS sale_hour,
    COUNT(*) AS total_orders,
    SUM(pos_order_total_amount) AS total_revenue,
    AVG(pos_order_total_amount) AS avg_order_value
FROM pos_orders
WHERE pos_order_status = 'COMPLETED'
AND pos_order_deleted_at IS NULL
GROUP BY DATE(pos_order_created_at), EXTRACT(HOUR FROM pos_order_created_at)
ORDER BY sale_date DESC, sale_hour;

CREATE OR REPLACE VIEW view_pos_top_products AS
SELECT 
    p.product_name,
    c.color_name,
    s.size_name,
    SUM(poi.pos_order_item_quantity) AS total_quantity_sold,
    SUM(poi.pos_order_item_total) AS total_revenue,
    COUNT(DISTINCT po.pos_order_id) AS number_of_orders
FROM pos_order_items poi
JOIN pos_orders po ON poi.pos_order_item_order_id = po.pos_order_id
JOIN product_details pd ON poi.pos_order_item_product_detail_id = pd.product_detail_id
JOIN products p ON pd.product_detail_product_id = p.product_id
JOIN colors c ON pd.product_detail_color_id = c.color_id
JOIN sizes s ON pd.product_detail_size_id = s.size_id
WHERE po.pos_order_status = 'COMPLETED'
AND po.pos_order_deleted_at IS NULL
GROUP BY p.product_name, c.color_name, s.size_name
ORDER BY total_quantity_sold DESC;

-- =====================================================
-- FUNCTION LOG ORDER STATUS CHANGE
-- =====================================================

CREATE OR REPLACE FUNCTION log_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.order_status IS DISTINCT FROM NEW.order_status THEN
        INSERT INTO order_histories (
            order_history_order_id,
            order_history_order_status,
            order_history_note,
            order_history_timestamp
        ) VALUES (
            NEW.order_id,
            NEW.order_status,
            'Trạng thái thay đổi từ "' || COALESCE(OLD.order_status, 'NULL') || '" thành "' || NEW.order_status || '"',
            NOW()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_log_order_status
AFTER UPDATE OF order_status ON orders
FOR EACH ROW
EXECUTE FUNCTION log_order_status_change();


select * from us


-- =====================================================
-- INSERT DỮ LIỆU MẪU CƠ BẢN (CHỈ 1 ADMIN)
-- =====================================================

-- 1. Tạo user admin (password: admin123)
-- Lưu ý: Thay 'your_hashed_password' bằng hash thực tế từ bcrypt
INSERT INTO users (username, email, password_hash, provider, status)
SELECT 'admin', 'admin@example.com', '$2a$10$Xy0.aIBH.YK/e1atkxjzUOVJYV6rqIiuB3aDqmMh32T839J9Pgg.W', 'LOCAL', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');


-- 2. Tạo roles (nếu chưa có)
INSERT INTO roles (role_name, status)
SELECT 'ADMIN', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ADMIN');

INSERT INTO roles (role_name, status)
SELECT 'STAFF', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'STAFF');

INSERT INTO roles (role_name, status)
SELECT 'CUSTOMER', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'CUSTOMER');

-- 3. Gán role ADMIN cho user admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin' 
  AND r.role_name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
);

-- 4. Tạo employee cho admin
INSERT INTO employees (
    employee_user_id,
    employee_fullname,
    employee_gender,
    employee_phone
)
SELECT 
    u.user_id,
    'Nguyễn Văn Admin',
    true,
    '0987654321'
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
    SELECT 1 FROM employees e 
    WHERE e.employee_user_id = u.user_id
);

-- 5. Thêm 1 customer user (tùy chọn, có thể bỏ qua nếu chỉ cần admin)
INSERT INTO users (username, email, password_hash, provider, status)
SELECT 'customer_demo', 'customer@example.com', '$2a$10$YourHashedPasswordHere', 'LOCAL', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'customer_demo');

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'customer_demo' 
  AND r.role_name = 'CUSTOMER'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
);

INSERT INTO customers (
    user_id,
    customer_name,
    phone,
    address
)
SELECT 
    u.user_id,
    'Khách Hàng Demo',
    '0909123456',
    '123 Đường Nguyễn Huệ, Quận 1, TP.HCM'
FROM users u
WHERE u.username = 'customer_demo'
  AND NOT EXISTS (
    SELECT 1 FROM customers c 
    WHERE c.user_id = u.user_id
);

-- 6. Danh mục sản phẩm
INSERT INTO categories (category_name, category_description)
SELECT 'Áo nam', 'Các loại áo thời trang nam'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Áo nam' AND category_status <> 'DELETED');

INSERT INTO categories (category_name, category_description)
SELECT 'Áo nữ', 'Các loại áo thời trang nữ'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Áo nữ' AND category_status <> 'DELETED');

INSERT INTO categories (category_name, category_description)
SELECT 'Quần nam', 'Các loại quần thời trang nam'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Quần nam' AND category_status <> 'DELETED');

INSERT INTO categories (category_name, category_description)
SELECT 'Quần nữ', 'Các loại quần thời trang nữ'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Quần nữ' AND category_status <> 'DELETED');

INSERT INTO categories (category_name, category_description)
SELECT 'Phụ kiện', 'Phụ kiện thời trang'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Phụ kiện' AND category_status <> 'DELETED');

-- 7. Sản phẩm
INSERT INTO products (product_name, product_description, product_category_id)
SELECT 'Áo thun nam cổ tròn', 'Áo thun nam chất liệu cotton cao cấp', c.category_id
FROM categories c
WHERE c.category_name = 'Áo nam'
  AND NOT EXISTS (
    SELECT 1 FROM products p 
    WHERE p.product_name = 'Áo thun nam cổ tròn' 
    AND p.product_status <> 'DELETED'
);

INSERT INTO products (product_name, product_description, product_category_id)
SELECT 'Áo sơ mi nam tay dài', 'Áo sơ mi nam form regular', c.category_id
FROM categories c
WHERE c.category_name = 'Áo nam'
  AND NOT EXISTS (
    SELECT 1 FROM products p 
    WHERE p.product_name = 'Áo sơ mi nam tay dài' 
    AND p.product_status <> 'DELETED'
);

INSERT INTO products (product_name, product_description, product_category_id)
SELECT 'Đầm nữ công sở', 'Đầm nữ phong cách công sở', c.category_id
FROM categories c
WHERE c.category_name = 'Áo nữ'
  AND NOT EXISTS (
    SELECT 1 FROM products p 
    WHERE p.product_name = 'Đầm nữ công sở' 
    AND p.product_status <> 'DELETED'
);

-- 8. Kích thước
INSERT INTO sizes (size_code, size_name, size_description)
SELECT 'S', 'Small', 'Size nhỏ'
WHERE NOT EXISTS (SELECT 1 FROM sizes WHERE size_code = 'S' AND size_status <> 'DELETED');

INSERT INTO sizes (size_code, size_name, size_description)
SELECT 'M', 'Medium', 'Size trung bình'
WHERE NOT EXISTS (SELECT 1 FROM sizes WHERE size_code = 'M' AND size_status <> 'DELETED');

INSERT INTO sizes (size_code, size_name, size_description)
SELECT 'L', 'Large', 'Size lớn'
WHERE NOT EXISTS (SELECT 1 FROM sizes WHERE size_code = 'L' AND size_status <> 'DELETED');

INSERT INTO sizes (size_code, size_name, size_description)
SELECT 'XL', 'Extra Large', 'Size rất lớn'
WHERE NOT EXISTS (SELECT 1 FROM sizes WHERE size_code = 'XL' AND size_status <> 'DELETED');

-- 9. Màu sắc
INSERT INTO colors (color_code, color_name, color_hex_code)
SELECT 'BLACK', 'Đen', '#000000'
WHERE NOT EXISTS (SELECT 1 FROM colors WHERE color_code = 'BLACK' AND color_status <> 'DELETED');

INSERT INTO colors (color_code, color_name, color_hex_code)
SELECT 'WHITE', 'Trắng', '#FFFFFF'
WHERE NOT EXISTS (SELECT 1 FROM colors WHERE color_code = 'WHITE' AND color_status <> 'DELETED');

INSERT INTO colors (color_code, color_name, color_hex_code)
SELECT 'RED', 'Đỏ', '#FF0000'
WHERE NOT EXISTS (SELECT 1 FROM colors WHERE color_code = 'RED' AND color_status <> 'DELETED');

INSERT INTO colors (color_code, color_name, color_hex_code)
SELECT 'BLUE', 'Xanh dương', '#0000FF'
WHERE NOT EXISTS (SELECT 1 FROM colors WHERE color_code = 'BLUE' AND color_status <> 'DELETED');

-- 10. Chi tiết sản phẩm
INSERT INTO product_details (
    product_detail_product_id,
    product_detail_color_id,
    product_detail_size_id,
    product_detail_sku,
    product_detail_price,
    product_detail_quantity
)
SELECT 
    p.product_id,
    c.color_id,
    s.size_id,
    sku_data.sku,
    sku_data.price,
    sku_data.quantity
FROM (
    VALUES 
        ('Áo thun nam cổ tròn', 'BLACK', 'S', 'TSN-S-BLACK', 150000, 100),
        ('Áo thun nam cổ tròn', 'BLACK', 'M', 'TSN-M-BLACK', 150000, 120),
        ('Áo thun nam cổ tròn', 'WHITE', 'S', 'TSN-S-WHITE', 150000, 80),
        ('Áo sơ mi nam tay dài', 'BLACK', 'M', 'ASN-M-BLACK', 250000, 60),
        ('Đầm nữ công sở', 'BLUE', 'M', 'DAM-M-BLUE', 450000, 30)
) AS sku_data(product_name, color_code, size_code, sku, price, quantity)
JOIN products p ON p.product_name = sku_data.product_name AND p.product_status <> 'DELETED'
JOIN colors c ON c.color_code = sku_data.color_code AND c.color_status <> 'DELETED'
JOIN sizes s ON s.size_code = sku_data.size_code AND s.size_status <> 'DELETED'
WHERE NOT EXISTS (
    SELECT 1 FROM product_details pd 
    WHERE pd.product_detail_sku = sku_data.sku
);

-- 11. Hình ảnh sản phẩm
INSERT INTO product_images (
    product_image_product_detail_id,
    product_image_path,
    product_image_is_main,
    product_image_display_order
)
SELECT 
    pd.product_detail_id,
    '/images/products/' || pd.product_detail_sku || '-main.jpg',
    true,
    1
FROM product_details pd
WHERE NOT EXISTS (
    SELECT 1 FROM product_images pi 
    WHERE pi.product_image_product_detail_id = pd.product_detail_id 
    AND pi.product_image_is_main = true
);

-- 12. Phương thức thanh toán
INSERT INTO payment_methods (method_name, description)
SELECT 'CASH', 'Thanh toán tiền mặt'
WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE method_name = 'CASH');

INSERT INTO payment_methods (method_name, description)
SELECT 'BANK_TRANSFER', 'Chuyển khoản ngân hàng'
WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE method_name = 'BANK_TRANSFER');

INSERT INTO payment_methods (method_name, description)
SELECT 'CREDIT_CARD', 'Thẻ tín dụng'
WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE method_name = 'CREDIT_CARD');

-- 13. Voucher
INSERT INTO vouchers (
    voucher_code,
    voucher_discount_amount,
    voucher_start_date,
    voucher_end_date,
    voucher_max_usage,
    voucher_min_order_value,
    voucher_status
)
SELECT 'WELCOME10', 50000, NOW(), NOW() + INTERVAL '30 days', 100, 200000, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM vouchers 
    WHERE voucher_code = 'WELCOME10' 
    AND voucher_status <> 'DELETED'
);

-- 14. Promotion
INSERT INTO promotions (
    promotion_name,
    promotion_description,
    promotion_start_date,
    promotion_end_date,
    promotion_discount_percent,
    promotion_status
)
SELECT 'Giảm giá mùa hè', 'Giảm 20% cho tất cả áo thun', NOW(), NOW() + INTERVAL '30 days', 20, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM promotions 
    WHERE promotion_name = 'Giảm giá mùa hè' 
    AND promotion_status <> 'DELETED'
);

-- 15. Cash Register
INSERT INTO cash_registers (
    cash_register_name,
    cash_register_location,
    cash_register_status,
    cash_register_opening_balance,
    cash_register_opened_by
)
SELECT 
    'Quầy thu ngân 1',
    'Tầng 1 - Khu A',
    'ACTIVE',
    5000000,
    e.employee_id
FROM employees e
WHERE e.employee_fullname = 'Nguyễn Văn Admin'
  AND NOT EXISTS (
    SELECT 1 FROM cash_registers cr 
    WHERE cr.cash_register_name = 'Quầy thu ngân 1'
);

-- 16. POS Session
INSERT INTO pos_sessions (
    pos_session_cash_register_id,
    pos_session_employee_id,
    pos_session_start_time,
    pos_session_status,
    pos_session_start_balance
)
SELECT 
    cr.cash_register_id,
    e.employee_id,
    NOW(),
    'ACTIVE',
    5000000
FROM cash_registers cr
CROSS JOIN employees e
WHERE cr.cash_register_name = 'Quầy thu ngân 1'
  AND e.employee_fullname = 'Nguyễn Văn Admin'
  AND NOT EXISTS (
    SELECT 1 FROM pos_sessions ps 
    WHERE ps.pos_session_status = 'ACTIVE'
);

-- 17. Invoice Sequences
INSERT INTO invoice_sequences (sequence_prefix, sequence_year, sequence_month, sequence_current_number)
SELECT 'INV', EXTRACT(YEAR FROM CURRENT_DATE), EXTRACT(MONTH FROM CURRENT_DATE), 1
WHERE NOT EXISTS (
    SELECT 1 FROM invoice_sequences 
    WHERE sequence_prefix = 'INV' 
    AND sequence_year = EXTRACT(YEAR FROM CURRENT_DATE)
    AND sequence_month = EXTRACT(MONTH FROM CURRENT_DATE)
);

INSERT INTO invoice_sequences (sequence_prefix, sequence_year, sequence_month, sequence_current_number)
SELECT 'POS', EXTRACT(YEAR FROM CURRENT_DATE), EXTRACT(MONTH FROM CURRENT_DATE), 1
WHERE NOT EXISTS (
    SELECT 1 FROM invoice_sequences 
    WHERE sequence_prefix = 'POS' 
    AND sequence_year = EXTRACT(YEAR FROM CURRENT_DATE)
    AND sequence_month = EXTRACT(MONTH FROM CURRENT_DATE)
);

-- =====================================================
-- KIỂM TRA KẾT QUẢ
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '========== KIỂM TRA DỮ LIỆU ==========';
    RAISE NOTICE 'Users: %', (SELECT COUNT(*) FROM users);
    RAISE NOTICE 'Roles: %', (SELECT COUNT(*) FROM roles);
    RAISE NOTICE 'Employees: %', (SELECT COUNT(*) FROM employees);
    RAISE NOTICE 'Customers: %', (SELECT COUNT(*) FROM customers);
    RAISE NOTICE 'Categories: %', (SELECT COUNT(*) FROM categories);
    RAISE NOTICE 'Products: %', (SELECT COUNT(*) FROM products);
    RAISE NOTICE 'Product Details: %', (SELECT COUNT(*) FROM product_details);
    RAISE NOTICE 'Payment Methods: %', (SELECT COUNT(*) FROM payment_methods);
    RAISE NOTICE 'Vouchers: %', (SELECT COUNT(*) FROM vouchers);
    RAISE NOTICE 'Promotions: %', (SELECT COUNT(*) FROM promotions);
    RAISE NOTICE 'Cash Registers: %', (SELECT COUNT(*) FROM cash_registers);
    RAISE NOTICE 'POS Sessions: %', (SELECT COUNT(*) FROM pos_sessions);
    RAISE NOTICE '========================================';
    RAISE NOTICE 'ĐÃ THÊM DỮ LIỆU MẪU THÀNH CÔNG!';
END $$;

select * from users
