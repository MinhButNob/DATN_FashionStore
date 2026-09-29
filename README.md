# Fashion Store

Fashion Store là hệ thống thương mại điện tử thời trang được xây dựng với kiến trúc Frontend – Backend, hỗ trợ quy trình mua bán trực tuyến và quản lý cửa hàng.

Hệ thống cung cấp các chức năng dành cho khách hàng, quản trị viên, nhân viên bán hàng và hỗ trợ tư vấn sản phẩm bằng AI.

## 1. Giới thiệu

Dự án được xây dựng nhằm mô phỏng một hệ thống kinh doanh thời trang thực tế, bao gồm:

* Website bán hàng trực tuyến.
* Hệ thống quản trị cửa hàng.
* Hệ thống bán hàng tại quầy (POS).
* Quản lý đơn hàng và hoàn hàng.
* Thanh toán trực tuyến.
* Quản lý sản phẩm và biến thể.
* Quản lý khách hàng, nhân viên và voucher.
* Thống kê và báo cáo.
* Tư vấn sản phẩm bằng AI.

## 2. Chức năng

### Khách hàng

* Đăng ký, đăng nhập và quản lý tài khoản.
* Xem, tìm kiếm và lọc sản phẩm.
* Xem chi tiết sản phẩm.
* Lựa chọn biến thể sản phẩm theo màu sắc, kích thước.
* Quản lý giỏ hàng.
* Quản lý địa chỉ giao hàng.
* Đặt hàng trực tuyến.
* Áp dụng voucher và chương trình khuyến mãi.
* Thanh toán trực tuyến qua VNPay.
* Theo dõi trạng thái đơn hàng.
* Hủy đơn hàng theo điều kiện cho phép.
* Yêu cầu hoàn hàng theo từng sản phẩm trong đơn.
* Theo dõi trạng thái yêu cầu hoàn hàng.
* Chat với AI để được tư vấn sản phẩm thời trang.

### Quản lý sản phẩm

* Quản lý sản phẩm.
* Quản lý danh mục.
* Quản lý biến thể sản phẩm.
* Quản lý màu sắc và kích thước.
* Quản lý giá và số lượng sản phẩm.
* Quản lý hình ảnh sản phẩm.
* Quản lý hình ảnh theo từng biến thể.
* Quản lý trạng thái sản phẩm.

### Quản lý đơn hàng

* Xem danh sách đơn hàng.
* Xem chi tiết đơn hàng.
* Cập nhật trạng thái đơn hàng.
* Quản lý trạng thái từng sản phẩm trong đơn hàng.
* Hỗ trợ xử lý đơn hàng có nhiều sản phẩm.
* Hủy đơn hàng.
* Xử lý yêu cầu hoàn hàng.
* Hỗ trợ hoàn một hoặc nhiều sản phẩm trong cùng một đơn hàng.
* Tính toán số tiền hoàn dựa trên số tiền thực tế khách hàng đã thanh toán cho sản phẩm.

### Bán hàng tại quầy (POS)

* Mở và đóng ca bán hàng.
* Tìm kiếm sản phẩm.
* Tìm kiếm khách hàng bằng số điện thoại.
* Thêm sản phẩm vào giỏ hàng.
* Áp dụng voucher.
* Thanh toán tại quầy.
* Thanh toán qua VNPay.
* Quản lý đơn hàng bán tại quầy.
* Xem và in hóa đơn.
* Hỗ trợ xử lý hoàn hàng tại quầy.

### Quản lý khách hàng và nhân viên

* Quản lý thông tin khách hàng.
* Quản lý tài khoản nhân viên.
* Phân quyền người dùng.
* Quản lý vai trò.
* Theo dõi lịch sử hoạt động liên quan đến đơn hàng.

### Voucher và khuyến mãi

* Quản lý voucher.
* Thiết lập mức giảm giá.
* Áp dụng voucher khi đặt hàng.
* Kiểm soát số lần sử dụng voucher.
* Tính toán giá sản phẩm sau khi áp dụng khuyến mãi và voucher.

### Thống kê

* Thống kê doanh thu.
* Thống kê đơn hàng.
* Thống kê sản phẩm.
* Theo dõi tình hình kinh doanh.
* Hiển thị dữ liệu trên Dashboard.

### AI Fashion Assistant

Hệ thống tích hợp AI nhằm hỗ trợ khách hàng lựa chọn sản phẩm thời trang.

AI có thể dựa trên các thông tin như:

* Giới tính.
* Độ tuổi.
* Chiều cao, cân nặng.
* Mục đích sử dụng.
* Ngân sách.
* Màu sắc yêu thích.
* Phong cách mong muốn.

Sau đó hệ thống sử dụng dữ liệu sản phẩm thực tế từ database để đưa ra các sản phẩm phù hợp.

## 3. Công nghệ sử dụng

### Backend

* Java
* Spring Boot
* Spring Data JPA
* Spring Security
* JWT
* PostgreSQL
* RESTful API
* Maven

### Frontend

* Vue 3
* Vite
* Vue Router
* Bootstrap 5
* Tailwind CSS
* JavaScript

### Authentication

* Spring Security
* JWT
* BCrypt
* Firebase Authentication

### Payment

* VNPay

### Shipping

* GHTK API

### AI

* OpenRouter API
* AI Chatbot tư vấn thời trang

### DevOps và công cụ

* Git
* GitHub
* Docker
* Docker Compose
* Railway
* Ngrok
* IntelliJ IDEA
* Visual Studio Code

## 4. Kiến trúc hệ thống

Hệ thống được tổ chức theo mô hình Client – API – Database:

```text
                    +-------------------+
                    |     Customer      |
                    |    Vue 3 Client   |
                    +---------+---------+
                              |
                              | REST API
                              v
                    +-------------------+
                    |    Spring Boot    |
                    |      Backend      |
                    +---------+---------+
                              |
          +-------------------+-------------------+
          |                   |                   |
          v                   v                   v
   +-------------+     +-------------+     +-------------+
   | PostgreSQL  |     |   VNPay     |     |    GHTK     |
   |  Database   |     |  Payment    |     |  Shipping   |
   +-------------+     +-------------+     +-------------+
                              |
                              v
                       +-------------+
                       | AI Service  |
                       |   Chatbot   |
                       +-------------+
```

## 5. Database

Hệ thống sử dụng PostgreSQL để lưu trữ dữ liệu.

Một số nhóm dữ liệu chính:

* Users
* Roles
* Customers
* Employees
* Products
* Product Details
* Categories
* Orders
* Order Details
* Order History
* Payments
* Vouchers
* Return Requests
* Return Items
* Shipping Addresses
* POS Sessions
* POS Payments

## 6. Quy trình đặt hàng

```text
Khách hàng
    |
    v
Xem sản phẩm
    |
    v
Thêm vào giỏ hàng
    |
    v
Nhập địa chỉ giao hàng
    |
    v
Áp dụng voucher
    |
    v
Tính phí vận chuyển
    |
    v
Tạo đơn hàng
    |
    v
Thanh toán
    |
    v
Xác nhận đơn hàng
    |
    v
Xử lý đơn hàng
    |
    v
Giao hàng
    |
    v
Hoàn thành
```

## 7. Quy trình hoàn hàng

Hệ thống hỗ trợ hoàn từng sản phẩm thay vì bắt buộc hoàn toàn bộ đơn hàng.

Ví dụ:

```text
Đơn hàng
├── Sản phẩm A → Hoàn hàng
├── Sản phẩm B → Hoàn hàng
└── Sản phẩm C → Hoàn thành
```

Trạng thái hoàn hàng được quản lý độc lập theo từng sản phẩm, giúp xử lý các trường hợp khách hàng chỉ muốn hoàn một phần đơn hàng.

## 8. Quy trình thanh toán

Hệ thống hỗ trợ nhiều phương thức thanh toán:

* Thanh toán khi nhận hàng.
* Thanh toán trực tuyến qua VNPay.
* Thanh toán tại quầy POS.

Đối với thanh toán VNPay, hệ thống tạo giao dịch thanh toán và xử lý kết quả trả về từ VNPay để cập nhật trạng thái thanh toán và đơn hàng.

## 9. Cài đặt và chạy dự án

### Yêu cầu môi trường

```text
Java 21
Node.js 20+
PostgreSQL
Maven
Git
```

### Clone project

```bash
git clone <repository-url>
cd DATN_FashionStore
```

### Chạy Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend mặc định chạy tại:

```text
http://localhost:8080
```

### Chạy Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend mặc định chạy tại:

```text
http://localhost:5173
```

## 10. Cấu hình môi trường

Tạo file `.env.properties` hoặc cấu hình các biến môi trường tương ứng:

```properties
DB_URL=jdbc:postgresql://localhost:5432/fashion_store
DB_USERNAME=postgres
DB_PASSWORD=your_password

JWT_SECRET=your_jwt_secret

VNPAY_TMN_CODE=your_vnpay_tmn_code
VNPAY_HASH_SECRET=your_vnpay_hash_secret

GHTK_TOKEN=your_ghtk_token
GHTK_CLIENT_SOURCE=your_client_source

OPENROUTER_API_KEY=your_openrouter_api_key
```

Không commit các thông tin bảo mật như:

```text
API Key
JWT Secret
Database Password
VNPay Secret
GHTK Token
```

vào GitHub.

## 11. Docker

Có thể sử dụng Docker Compose để chạy các thành phần của hệ thống:

```bash
docker compose up -d
```

Dừng hệ thống:

```bash
docker compose down
```

## 12. Cấu trúc dự án

```text
DATN_FashionStore/
│
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       └── resources/
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── vite.config.js
│   └── Dockerfile
│
├── docker-compose.yml
└── README.md
```

## 13. Mục tiêu dự án

Dự án được xây dựng nhằm áp dụng kiến thức vào một hệ thống thương mại điện tử thực tế, đặc biệt tập trung vào:

* Xây dựng RESTful API với Spring Boot.
* Thiết kế và quản lý cơ sở dữ liệu PostgreSQL.
* Xác thực và phân quyền người dùng.
* Xử lý quy trình đặt hàng và thanh toán.
* Xử lý hoàn hàng một phần.
* Tích hợp các API bên thứ ba.
* Xây dựng hệ thống bán hàng tại quầy.
* Tích hợp AI vào hệ thống thương mại điện tử.
* Triển khai ứng dụng bằng Docker.

## 14. Tác giả

**Đặng Tuấn Minh**
**Nhóm SD-04**

Dự án tốt nghiệp – FPT Polytechnic.

---

## License

This project is developed for educational and demonstration purposes.
