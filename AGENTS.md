# AGENTS.md

## 1. Tổng quan dự án

Tên dự án: Fashion Store

Loại dự án:

* Website bán quần áo
* Kiến trúc REST API
* Frontend và Backend tách biệt

Công nghệ:

### Backend

* Java 21
* Spring Boot 3
* Spring Security + JWT
* Spring Data JPA
* PostgreSQL
* Maven
* Lombok

### Frontend

* VueJS 3
* Pinia
* Vue Router
* Axios

### Authentication

* JWT Authentication
* Google Login
* Facebook Login

---

# 2. Kiến trúc Backend

Package structure:

com.example.backend_fashionstore

├── auth
│   ├── controller
│   ├── dto
│   ├── repository
│   └── service
│
├── config
│   └── SecurityConfig
│
├── entity
│
├── security
│   ├── JwtService
│   ├── JwtAuthenticationFilter
│   └── CustomUserDetailsService
│
├── repository
│
├── service
│
├── dto
│
└── controller

---

# 3. Quy tắc đặt tên

## Entity

Tên entity dùng PascalCase.

Ví dụ:

* User
* Role
* Customer
* Employee
* Category
* Product
* Size
* Color

Không được đặt:

* Users
* Categories
* Products

---

## DTO

Tên DTO phải có hậu tố:

Request:

* CreateProductRequest
* UpdateProductRequest

Response:

* ProductResponse
* UserProfileResponse

Không dùng:

* ProductDTO
* UserDTO

trừ khi được yêu cầu.

---

## Repository

Luôn kế thừa:

JpaRepository<Entity, Integer>

Ví dụ:

public interface SizeRepository
extends JpaRepository<Size, Integer>

---

## Service

Phải tách:

* Interface
* Implementation

Ví dụ:

SizeService
SizeServiceImpl

Không viết business logic trong Controller.

---

## Controller

Chỉ xử lý:

* Request
* Response
* Validation

Không chứa business logic.

---

# 4. Security Rules

Authentication:

JWT Bearer Token

Header:

Authorization: Bearer <token>

---

Role hiện tại:

ADMIN
STAFF
CUSTOMER

---

Phân quyền:

ADMIN:

* Quản lý toàn bộ hệ thống

STAFF:

* Quản lý đơn hàng
* Quản lý sản phẩm

CUSTOMER:

* Mua hàng
* Giỏ hàng
* Đơn hàng cá nhân

---

Security ưu tiên:

@PreAuthorize

Ví dụ:

@PreAuthorize("hasRole('ADMIN')")

@PreAuthorize("hasAnyRole('ADMIN','STAFF')")

Không hardcode role trong Controller.

---

# 5. Quy tắc Database

Database:

PostgreSQL

---

Mọi bảng đều hỗ trợ:

created_at
updated_at
deleted_at

---

Soft Delete

KHÔNG dùng:

DELETE FROM table

Thay bằng:

status='DELETED'
deleted_at=NOW()

---

Khi query dữ liệu:

Luôn bỏ qua bản ghi đã xóa.

Ví dụ:

status <> 'DELETED'
AND deleted_at IS NULL

---

# 6. Status Convention

## User

ACTIVE
INACTIVE
BANNED
DELETED

---

## Role

ACTIVE
INACTIVE
DELETED

---

## Customer

Không có status riêng.

Customer phụ thuộc trạng thái User.

---

## Employee

Không có status riêng.

Employee phụ thuộc trạng thái User.

---

## Size

ACTIVE
INACTIVE
DELETED

---

## Category

ACTIVE
INACTIVE
DELETED

---

## Color

ACTIVE
INACTIVE
DELETED

---

## Product

ACTIVE
INACTIVE
DELETED

---

# 7. Entity Mapping Rules

User

@OneToMany
User -> UserRole

@OneToOne
User -> Customer

@OneToOne
User -> Employee

---

Product

@ManyToOne
Product -> Category

---

ProductDetail

@ManyToOne
ProductDetail -> Product

@ManyToOne
ProductDetail -> Size

@ManyToOne
ProductDetail -> Color

---

# 8. Coding Style

Bắt buộc dùng:

Lombok

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

---

Không viết:

public String getName(){}

nếu Lombok đã xử lý.

---

Constructor Injection:

@RequiredArgsConstructor

Không dùng:

@Autowired field injection

---

# 9. API Response

Response dạng:

{
"success": true,
"message": "Success",
"data": {}
}

Ưu tiên ResponseEntity.

---

# 10. Khi AI tạo CRUD

Luôn tạo đủ:

1. Entity
2. Repository
3. Service
4. ServiceImpl
5. Request DTO
6. Response DTO
7. Controller

---

# 11. Khi thêm cột mới vào Database

Ví dụ:

status VARCHAR(20)

AI phải kiểm tra và cập nhật:

* Entity
* DTO
* Repository Query
* Service
* Response
* Validation

Không được chỉ sửa Entity.

---

# 12. Git Rules

Trước khi push:

git pull --rebase origin main

Nếu conflict:

1. Giữ logic mới nhất
2. Xóa toàn bộ:

# <<<<<<< HEAD

> > > > > > >

3. Build lại project
4. Commit
5. Push

---

# 13. Docker Rules

Mọi cấu hình mới phải tương thích:

* Docker
* Docker Compose

Không hardcode:

localhost

Ưu tiên:

Environment Variables

---

# 14. Nguyên tắc quan trọng

AI KHÔNG được:

* Đổi tên bảng
* Đổi tên cột
* Đổi package
* Đổi API đang hoạt động

trừ khi được yêu cầu rõ ràng.

AI phải ưu tiên:

* Tương thích code hiện tại
* Không phá vỡ API cũ
* Không tạo duplicate repository
* Không tạo duplicate entity
* Không tạo duplicate package
