# QuickDeliver — Backend

City-scoped quick delivery platform for electronics. Spring Boot 3 + MySQL monolith.

---

## Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Fast2SMS account (free) → https://www.fast2sms.com

---

## Setup

### 1. Create the MySQL database
```sql
CREATE DATABASE quickdeliver CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Fill in application.properties
```properties
spring.datasource.password=YOUR_MYSQL_PASSWORD

razorpay.key.id=rzp_test_XXXXXXXX
razorpay.key.secret=XXXXXXXXXXXXXXXX

fast2sms.api.key=YOUR_FAST2SMS_KEY       ← from fast2sms.com dashboard

geocoding.nominatim.user-agent=QuickDeliver/1.0 (your@email.com)
```

### 3. Run
```bash
mvn spring-boot:run
```
Server: `http://localhost:8080/api`

---

## First Boot — what gets auto-seeded
| Data | Details |
|------|---------|
| 10 cities | Agra, Delhi, Jaipur, Lucknow, Kanpur, Mathura, Noida, Gurgaon, Varanasi, Allahabad — each with real lat/lng |
| 15 categories | Headphones, Smartphones, Laptops, Refrigerators, AC, etc. |
| Admin user | **None** — register yourself via OTP then promote in DB |

### Making yourself ADMIN
```sql
-- After you register via the normal OTP flow:
UPDATE users SET role = 'ADMIN' WHERE phone = '9XXXXXXXXX';
```

---

## Location Flow (how city detection works)

```
Browser → asks permission
    ↓ granted
Frontend sends { latitude, longitude }
    ↓
POST /api/location/resolve
    ↓
Backend runs Haversine SQL query against cities table (radius = 50km)
    ↓ match found
Returns { matched: true, cityId, cityName }   → frontend loads products for that city
    ↓ no match
Backend calls Nominatim reverse geocoding to get place name
Returns { matched: false, detectedCityName, availableCities[] }  → show manual picker

User picks manually → POST /api/location/select/{cityId}
```

---

## OTP Flow (real SMS via Fast2SMS)

```
POST /api/auth/send-otp    { "phone": "9876543210" }
    ↓
- Creates user if new
- Generates 6-digit OTP
- Saves OTP + 5-min expiry to DB
- 60-second cooldown between resend attempts
- Calls Fast2SMS API → SMS delivered to phone
    ↓
POST /api/auth/verify-otp  { "phone": "9876543210", "otp": "482910", "name": "Ravi" }
    ↓
- Validates OTP + expiry
- Clears OTP (single use)
- Returns JWT token (valid 24h)
```

---

## Full API Reference

### Public — no auth needed
| Method | Endpoint | Notes |
|--------|----------|-------|
| POST | `/auth/send-otp` | Send OTP SMS |
| POST | `/auth/verify-otp` | Verify OTP → JWT |
| POST | `/location/resolve` | `{latitude, longitude}` → matched city or city list |
| POST | `/location/select/{cityId}` | Manual city pick |
| GET | `/cities` | All active cities |
| GET | `/categories` | All active categories |
| GET | `/products?cityId=&categoryId=&q=` | Browse / search products |
| GET | `/products/{id}` | Product detail + shop manager phone |
| GET | `/shops/public?cityId=` | Active shops in city |
| GET | `/shops/public/{id}` | Shop detail |

### User — Bearer token (role: USER)
| Method | Endpoint | Notes |
|--------|----------|-------|
| POST | `/orders` | Place order |
| GET | `/orders/my` | Order history |
| GET | `/orders/{id}` | Order detail |
| POST | `/payments/upi/create/{orderId}` | Get Razorpay checkout params |
| POST | `/payments/upi/verify` | Confirm payment after checkout |

### Admin / Member — Bearer token (role: ADMIN or MEMBER)
| Method | Endpoint | Notes |
|--------|----------|-------|
| POST | `/admin/shops` | Register new shop |
| PUT | `/admin/shops/{id}` | Update shop |
| PUT | `/admin/shops/{id}/status?status=ACTIVE` | Approve / suspend (ADMIN only) |
| GET | `/admin/shops` | All shops |
| POST | `/admin/shops/{shopId}/products` | Add product |
| PUT | `/admin/products/{id}` | Edit product |
| DELETE | `/admin/products/{id}` | Deactivate product |
| PUT | `/admin/products/{id}/stock?quantity=50` | Update stock |
| GET | `/admin/orders` | All orders (ADMIN only) |
| PUT | `/admin/orders/{id}/status?status=OUT_FOR_DELIVERY` | Update order status |
| POST | `/payments/cod/collected/{orderId}` | Mark COD collected → DELIVERED |
| POST | `/cities` | Add new city (ADMIN only) |
| PUT | `/categories/{id}` | Edit category (ADMIN only) |

---

## Payment Flows

### UPI (Razorpay)
1. `POST /orders` → get `orderId`
2. `POST /payments/upi/create/{orderId}` → get `{ razorpayOrderId, keyId, amount }`
3. Open Razorpay checkout JS with those values
4. On success callback → `POST /payments/upi/verify` with `{ razorpayOrderId, razorpayPaymentId, razorpaySignature }`
5. Backend HMAC-verifies signature → order → `CONFIRMED`

### COD
1. `POST /orders` with `"paymentMethod": "COD"` → instantly `CONFIRMED`
2. Delivery agent calls `POST /payments/cod/collected/{orderId}` → `DELIVERED` + payment `SUCCESS`

---

## Adding a New City
```bash
POST /api/cities
Authorization: Bearer <ADMIN_TOKEN>
{
  "name": "Meerut",
  "state": "Uttar Pradesh",
  "latitude": 28.9845,
  "longitude": 77.7064,
  "active": true
}
```

---

## Project Structure
```
src/main/java/com/quickdeliver/
├── QuickDeliverApplication.java
├── config/
│   ├── SecurityConfig.java       JWT + CORS + role access
│   ├── WebClientConfig.java      WebClient bean for HTTP calls
│   └── DataSeeder.java           Seeds cities (with coords) + categories
├── controller/
│   ├── AuthController.java       OTP send + verify
│   ├── LocationController.java   Geolocation resolve + manual select
│   ├── CityController.java
│   ├── CategoryController.java
│   ├── ShopController.java       Public browsing
│   ├── ProductController.java    Browse + search by city
│   ├── OrderController.java
│   ├── PaymentController.java    UPI + COD
│   └── AdminController.java      Shop/product/order management
├── service/
│   ├── AuthService.java          OTP logic + JWT issue
│   ├── SmsService.java           Fast2SMS real SMS dispatch
│   ├── GeocodingService.java     Nominatim reverse geocoding
│   ├── LocationService.java      Haversine match + fallback
│   ├── ShopService.java
│   ├── ProductService.java
│   ├── OrderService.java
│   └── PaymentService.java       Razorpay HMAC verify + COD
├── entity/                       User, City(+lat/lng), Category, Shop,
│                                 Product, Order, OrderItem, Payment
├── repository/                   Includes Haversine native query in CityRepository
├── dto/request/                  LocationRequest, SendOtpRequest, ...
├── dto/response/                 LocationResponse, AuthResponse, ...
├── enums/                        Role, ShopStatus, OrderStatus, PaymentMethod, PaymentStatus
├── exception/                    GlobalExceptionHandler + custom exceptions
├── security/                     JwtAuthFilter
└── util/                         JwtUtil, OtpUtil
```
