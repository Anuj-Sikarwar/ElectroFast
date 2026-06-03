# QuickDeliver — Backend

City-scoped quick delivery platform for electronics. Spring Boot 3 + MySQL monolith.

---

## Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+

---

## Setup

### 1. Create the MySQL database
```sql
CREATE DATABASE quickdeliver CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Set environment variables
All configuration is via environment variables. Never hardcode secrets.

Required variables:
```
DB_URL
DB_USER
DB_PASSWORD
JWT_SECRET
PHONEPE_CLIENT_ID
PHONEPE_CLIENT_SECRET
PHONEPE_BASE_URL
PHONEPE_REDIRECT_URL
PHONEPE_CALLBACK_URL
FAST2SMS_KEY
NOMINATIM_USER_AGENT
TELEGRAM_BOT_TOKEN
TELEGRAM_CHAT_ID
```

### 3. Run locally
```bash
mvn spring-boot:run
```
Server starts at: `http://localhost:8080/api`

---

## First Boot
On startup the app automatically seeds:
- 10 cities with real coordinates (Agra, Delhi, Jaipur, Lucknow, Kanpur, Mathura, Noida, Gurgaon, Varanasi, Allahabad)
- 15 electronics categories

No admin user is seeded. Register yourself via the app then promote in DB:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';
```

---

## Auth
Email + password authentication. BCrypt password hashing. JWT token valid 24 hours.

```
POST /api/auth/register  { name, email, phone, password }
POST /api/auth/login     { email, password }
```

---

## Location Flow
```
Browser geolocation → POST /api/location/resolve { latitude, longitude }
→ Haversine SQL finds nearest city within 50km
→ Returns matched city or all cities for manual selection
```

---

## Payment
PhonePe Payment Gateway (OAuth2 API) + Cash on Delivery.

---

## Notifications
Telegram bot sends instant notification to platform owner on every order.

---

## API Reference

### Public
| Method | Endpoint | Notes |
|--------|----------|-------|
| POST | `/auth/register` | Register |
| POST | `/auth/login` | Login → JWT |
| POST | `/location/resolve` | Detect city |
| POST | `/location/select/{cityId}` | Manual city pick |
| GET | `/cities` | All active cities |
| GET | `/categories` | All categories |
| GET | `/products?cityId=&categoryId=&q=` | Browse / search |
| GET | `/products/{id}` | Product detail |
| GET | `/shops/public?cityId=` | Shops in city |

### User
| Method | Endpoint | Notes |
|--------|----------|-------|
| POST | `/orders` | Place order |
| GET | `/orders/my` | Order history |
| GET | `/orders/{id}` | Order detail |
| POST | `/payments/phonepe/initiate/{orderId}` | Initiate payment |
| GET | `/payments/phonepe/status/{txId}` | Check status |

### Delivery (role: DELIVERY)
| Method | Endpoint | Notes |
|--------|----------|-------|
| GET | `/delivery/available` | Available orders |
| POST | `/delivery/accept/{orderId}` | Accept order |
| GET | `/delivery/my` | My deliveries |
| PUT | `/delivery/{orderId}/out-for-delivery` | Mark out for delivery |
| PUT | `/delivery/{orderId}/delivered` | Mark delivered |

### Admin / Member (role: ADMIN or MEMBER)
| Method | Endpoint | Notes |
|--------|----------|-------|
| POST | `/admin/shops` | Register shop |
| PUT | `/admin/shops/{id}` | Update shop |
| PUT | `/admin/shops/{id}/status` | Approve / suspend |
| GET | `/admin/shops` | All shops |
| POST | `/admin/shops/{shopId}/products` | Add product |
| PUT | `/admin/products/{id}` | Edit product |
| DELETE | `/admin/products/{id}` | Deactivate product |
| PUT | `/admin/products/{id}/stock` | Update stock |
| GET | `/admin/orders` | All orders with filters |
| PUT | `/admin/orders/{id}/status` | Update order status |
| PUT | `/admin/orders/{id}/assign/{deliveryUserId}` | Assign delivery |
| GET | `/admin/delivery/personnel` | All delivery staff |
| POST | `/cities` | Add city (ADMIN only) |
| PUT | `/categories/{id}` | Edit category (ADMIN only) |
| POST | `/payments/cod/collected/{orderId}` | Mark COD collected |

---

## Roles
| Role | Who | Access |
|------|-----|--------|
| USER | Customer | Browse, order, pay |
| MEMBER | Field staff | Register shops, manage products |
| DELIVERY | Delivery person | Accept and deliver orders |
| ADMIN | Platform owner | Everything |

---

## Project Structure
```
src/main/java/com/quickdeliver/
├── config/          SecurityConfig, DataSeeder, WebClientConfig
├── controller/      Auth, Location, City, Category, Shop, Product,
│                    Order, Payment, Admin, Delivery
├── service/         Auth, Shop, Product, Order, Payment, Delivery,
│                    Location, Geocoding, Notification
├── entity/          User, City, Category, Shop, Product,
│                    Order, OrderItem, Payment
├── repository/
├── dto/             request/ and response/
├── enums/           Role, ShopStatus, OrderStatus, PaymentMethod, PaymentStatus
├── exception/       GlobalExceptionHandler + custom exceptions
├── security/        JwtAuthFilter
└── util/            JwtUtil, InputSanitizer
```
