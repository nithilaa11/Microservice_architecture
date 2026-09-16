# E-Commerce Microservices Architecture Application

> **Distributed Computing Hands-On Assignment**  
> Complete working implementation of an Amazon-like microservices platform built with **Java 21**, **Spring Boot 3.3.4**, **Spring Cloud 2023.0.3**, **Eureka Service Discovery**, **Spring Cloud API Gateway**, **OpenFeign**, and **Spring Data JPA**.

---

## 1. System Architecture

```mermaid
flowchart TD
    Client["Client / Postman / Frontend"] -->|HTTP Requests| Gateway["API Gateway (Port 8085)"]
    Gateway -->|Service Discovery Lookup| Eureka["Eureka Discovery Server (Port 8761)"]

    Gateway -->|lb://USER-SERVICE| US["User Service (Port 8081)"]
    Gateway -->|lb://PRODUCT-SERVICE| PS["Product Service (Port 8082)"]
    Gateway -->|lb://ORDER-SERVICE| OS["Order Service (Port 8083)"]
    Gateway -->|lb://PAYMENT-SERVICE| PayS["Payment Service (Port 8084)"]

    OS -->|OpenFeign: GET /api/users/{id}| US
    OS -->|OpenFeign: GET /api/products/{id} & PUT reduce-stock| PS
    OS -->|OpenFeign: POST /api/payments/process| PayS

    US --- DB1[("User DB (H2 / MySQL)")]
    PS --- DB2[("Product DB (H2 / MySQL)")]
    OS --- DB3[("Order DB (H2 / MySQL)")]
    PayS --- DB4[("Payment DB (H2 / MySQL)")]
```

### Microservices & Ports

| Service | Port | Database (H2 In-Memory Default) | Database (MySQL Profile) | Role |
|---|---|---|---|---|
| **Service Discovery** | `8761` | *None* | *None* | Central service registry (Netflix Eureka) |
| **API Gateway** | `8085` | *None* | *None* | Centralized entry point, routing, CORS & logging |
| **User Service** | `8081` | `ecommerce_users` | `ecommerce_users` | Customer profiles & registration |
| **Product Service** | `8082` | `ecommerce_products` | `ecommerce_products` | Catalog & inventory stock management |
| **Order Service** | `8083` | `ecommerce_orders` | `ecommerce_orders` | Checkout orchestration (OpenFeign client) |
| **Payment Service** | `8084` | `ecommerce_payments` | `ecommerce_payments` | Mock payment gateway with transaction tracking |

---

## 2. Distributed Computing Concepts Demonstrated

1. **Microservices Architecture**: The system is partitioned into independent, loosely-coupled domain services rather than a monolithic application.
2. **Independent Services**: Each service has its own codebase, build lifecycle (`pom.xml`), dependencies, and execution runtime.
3. **Dynamic Service Discovery (Netflix Eureka)**: Microservices register their network locations at startup; services locate each other dynamically using service IDs (`USER-SERVICE`, `PRODUCT-SERVICE`, etc.) rather than hard-coded IPs or ports.
4. **API Gateway Pattern**: Single entry point (`http://localhost:8085`) that encapsulates the internal system topology, handles reverse proxying, dynamic load-balanced routing, and centralized logging.
5. **REST API Communication**: Standardized HTTP verbs (`GET`, `POST`, `PUT`, `DELETE`) with JSON serialization.
6. **Inter-Service Communication via OpenFeign**: Declarative REST client inside `order-service` calling User, Product, and Payment microservices synchronously.
7. **Database per Service Pattern**: Each microservice exclusively owns its database schema (`ecommerce_users`, `ecommerce_products`, `ecommerce_orders`, `ecommerce_payments`). Cross-service data is joined through service APIs, not SQL `JOIN`s or foreign keys.
8. **Independent CRUD Operations**: Services can perform standalone create, read, update, delete actions on their own data models without impacting other services.
9. **SAGA Distributed Transaction & Compensation**: When placing an order:
   - User is validated.
   - Product stock is checked and reserved.
   - Payment is processed.
   - If payment fails, the **compensation logic** automatically restores product inventory and flags the order as `PAYMENT_FAILED`.
10. **Fault Tolerance & Service Downtime Handling**: If a downstream service (`payment-service` or `user-service`) becomes unavailable, Feign client exceptions are intercepted gracefully to return standard HTTP 503 Service Unavailable responses and safely restore inventory.

---

## 3. Project Directory Structure

```text
Microservice_architecture/
├── pom.xml                     # Root parent POM (aggregates all 6 modules)
├── mvnw.cmd                    # Maven Windows runner
├── start-all.ps1               # Launch all 6 services with 1 command
├── stop-all.ps1                # Stop all microservices by port
├── schema.sql                  # MySQL schemas & sample seed data
├── postman_collection.json     # Ready-to-import Postman collection
├── README.md                   # Complete documentation
│
├── service-discovery/          # Eureka Server (Port 8761)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/discovery/ServiceDiscoveryApplication.java
│       └── resources/application.yml
│
├── api-gateway/                # Spring Cloud Gateway (Port 8085)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/gateway/
│       │   ├── ApiGatewayApplication.java
│       │   └── filter/LoggingFilter.java
│       └── resources/application.yml
│
├── user-service/               # User Management (Port 8081)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/user/
│       │   ├── UserServiceApplication.java
│       │   ├── config/DataInitializer.java
│       │   ├── controller/UserController.java
│       │   ├── dto/UserDTO.java, ErrorResponse.java
│       │   ├── exception/GlobalExceptionHandler.java
│       │   ├── model/User.java
│       │   ├── repository/UserRepository.java
│       │   └── service/UserService.java, UserServiceImpl.java
│       └── resources/application.yml
│
├── product-service/            # Product & Inventory (Port 8082)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/product/
│       │   ├── ProductServiceApplication.java
│       │   ├── config/DataInitializer.java
│       │   ├── controller/ProductController.java
│       │   ├── dto/ProductDTO.java, StockResponseDTO.java, ErrorResponse.java
│       │   ├── exception/GlobalExceptionHandler.java, InsufficientStockException.java
│       │   ├── model/Product.java
│       │   ├── repository/ProductRepository.java
│       │   └── service/ProductService.java, ProductServiceImpl.java
│       └── resources/application.yml
│
├── payment-service/            # Mock Payment Gateway (Port 8084)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/payment/
│       │   ├── PaymentServiceApplication.java
│       │   ├── controller/PaymentController.java
│       │   ├── dto/PaymentRequestDTO.java, PaymentResponseDTO.java, ErrorResponse.java
│       │   ├── exception/GlobalExceptionHandler.java
│       │   ├── model/Payment.java
│       │   ├── repository/PaymentRepository.java
│       │   └── service/PaymentService.java, PaymentServiceImpl.java
│       └── resources/application.yml
│
└── order-service/              # Order Orchestration & OpenFeign (Port 8083)
    ├── pom.xml
    └── src/main/
        ├── java/com/ecommerce/order/
        │   ├── OrderServiceApplication.java
        │   ├── client/UserClient.java, ProductClient.java, PaymentClient.java
        │   ├── controller/OrderController.java
        │   ├── dto/OrderRequestDTO.java, OrderResponseDTO.java, UserDTO.java, ProductDTO.java, etc.
        │   ├── exception/GlobalExceptionHandler.java, ServiceUnavailableException.java
        │   ├── model/Order.java
        │   ├── repository/OrderRepository.java
        │   └── service/OrderService.java, OrderServiceImpl.java
        └── resources/application.yml
```

---

## 4. How to Run the Project

### Option A: Quick Launch (Automated PowerShell Script)
From the project root:
```powershell
.\start-all.ps1
```
This script launches Eureka first, followed by User, Product, Payment, Order services, and finally the API Gateway.

To stop all services:
```powershell
.\stop-all.ps1
```

### Option B: Build and Run Manually (Terminal / Command Prompt)

1. **Build all modules**:
   ```cmd
   mvnw.cmd clean package -DskipTests
   ```
2. **Start Services in separate terminals** (in order):
   - Terminal 1 (Eureka):
     ```cmd
     java -jar service-discovery\target\service-discovery-1.0.0.jar
     ```
   - Terminal 2 (User Service):
     ```cmd
     java -jar user-service\target\user-service-1.0.0.jar
     ```
   - Terminal 3 (Product Service):
     ```cmd
     java -jar product-service\target\product-service-1.0.0.jar
     ```
   - Terminal 4 (Payment Service):
     ```cmd
     java -jar payment-service\target\payment-service-1.0.0.jar
     ```
   - Terminal 5 (Order Service):
     ```cmd
     java -jar order-service\target\order-service-1.0.0.jar
     ```
   - Terminal 6 (API Gateway):
     ```cmd
     java -jar api-gateway\target\api-gateway-1.0.0.jar
     ```

### Database Switch: H2 vs. MySQL
- **H2 (Default)**: Runs completely in-memory with zero installation. Tables and pre-seeded sample data are loaded automatically.
- **MySQL**: 
  1. Execute `schema.sql` in MySQL to create databases:
     ```cmd
     mysql -u root -p < schema.sql
     ```
  2. Launch any service with the `mysql` profile:
     ```cmd
     java -jar user-service\target\user-service-1.0.0.jar --spring.profiles.active=mysql
     ```

---

## 5. Complete API Reference

All requests can be sent directly to the **API Gateway** (`http://localhost:8085`):

| HTTP Method | Endpoint | Service Handled | Purpose | Sample Request Body | Sample Response Body |
|---|---|---|---|---|---|
| `GET` | `http://localhost:8761` | Service Discovery | Eureka Web UI | *None* | HTML Dashboard |
| `GET` | `/api/users` | User Service | Get all registered users | *None* | `[{"id":1,"name":"Alice Johnson","email":"alice@example.com","phone":"9876543210"}, ...]` |
| `GET` | `/api/users/{id}` | User Service | Get user by ID | *None* | `{"id":1,"name":"Alice Johnson","email":"alice@example.com","phone":"9876543210"}` |
| `POST` | `/api/users` | User Service | Register new customer | `{"name":"Diana Prince","email":"diana@example.com","phone":"9876543299"}` | `{"id":4,"name":"Diana Prince","email":"diana@example.com","phone":"9876543299"}` |
| `PUT` | `/api/users/{id}` | User Service | Update customer profile | `{"name":"Diana P.","email":"diana@example.com","phone":"9876543299"}` | `{"id":4,"name":"Diana P.","email":"diana@example.com","phone":"9876543299"}` |
| `DELETE` | `/api/users/{id}` | User Service | Delete customer | *None* | `{"message":"User deleted successfully with id: 4"}` |
| `GET` | `/api/products` | Product Service | Get all products | *None* | `[{"id":1,"name":"Apple iPhone 15","price":799.99,"quantity":50}, ...]` |
| `GET` | `/api/products/{id}` | Product Service | Get product by ID | *None* | `{"id":1,"name":"Apple iPhone 15","description":"...","price":799.99,"quantity":50}` |
| `GET` | `/api/products/{id}/stock` | Product Service | Check inventory stock | *None* | `{"productId":1,"productName":"Apple iPhone 15","availableQuantity":50,"inStock":true}` |
| `POST` | `/api/products` | Product Service | Add product | `{"name":"MacBook Air M3","description":"16GB RAM","price":1299.99,"quantity":25}` | `{"id":5,"name":"MacBook Air M3","price":1299.99,"quantity":25}` |
| `PUT` | `/api/products/{id}/reduce-stock?quantity=2` | Product Service | Reduce inventory | *None* | `{"productId":1,"productName":"Apple iPhone 15","availableQuantity":48,"inStock":true}` |
| `PUT` | `/api/products/{id}/restore-stock?quantity=2` | Product Service | Restore inventory | *None* | `{"productId":1,"productName":"Apple iPhone 15","availableQuantity":50,"inStock":true}` |
| `POST` | `/api/orders` | Order Service | **Place Order (Orchestration Flow)** | `{"userId":1,"productId":1,"quantity":2,"paymentMethod":"CREDIT_CARD"}` | `{"orderId":1,"userId":1,"userName":"Alice Johnson","productId":1,"productName":"Apple iPhone 15","quantity":2,"totalAmount":1599.98,"status":"CONFIRMED","paymentReference":"TXN-XXXX","paymentStatus":"SUCCESS","message":"Order created and confirmed successfully!"}` |
| `POST` | `/api/orders` (Failure test) | Order Service | **Test SAGA Compensation** | `{"userId":1,"productId":1,"quantity":2,"paymentMethod":"CREDIT_CARD","simulatePaymentFailure":true}` | `{"orderId":2,"status":"PAYMENT_FAILED","paymentStatus":"FAILED","message":"Order payment failed (...). Inventory has been restored."}` |
| `GET` | `/api/orders` | Order Service | Get all purchase orders | *None* | `[{"orderId":1,"userId":1,"userName":"Alice Johnson","totalAmount":1599.98,"status":"CONFIRMED", ...}]` |
| `GET` | `/api/orders/{id}` | Order Service | Get order receipt by ID | *None* | `{"orderId":1,"userId":1,"userName":"Alice Johnson","totalAmount":1599.98,"status":"CONFIRMED", ...}` |
| `PUT` | `/api/orders/{id}/cancel` | Order Service | Cancel order & restore inventory | *None* | `{"orderId":1,"status":"CANCELLED","message":"Order cancelled successfully and product inventory restored."}` |
| `POST` | `/api/payments/process` | Payment Service | Process payment mock | `{"orderId":1,"amount":1599.98,"paymentMethod":"CREDIT_CARD"}` | `{"paymentId":1,"orderId":1,"amount":1599.98,"paymentStatus":"SUCCESS","transactionReference":"TXN-XXXX"}` |
| `GET` | `/api/payments/order/{orderId}` | Payment Service | Get payment for order | *None* | `{"paymentId":1,"orderId":1,"amount":1599.98,"paymentStatus":"SUCCESS", ...}` |

---

## 6. Hands-On Demonstration Workflow for Presentation

Follow these steps during your lab evaluation / presentation:

1. **Demonstrate Service Discovery**:
   - Open browser at `http://localhost:8761`.
   - Show that **API-GATEWAY**, **USER-SERVICE**, **PRODUCT-SERVICE**, **ORDER-SERVICE**, and **PAYMENT-SERVICE** are all registered and `UP`.
2. **Demonstrate API Gateway Centralization**:
   - Point out that every client request goes to port `8085` (`/api/users`, `/api/products`, `/api/orders`), and Gateway dynamic routing delegates to the internal microservices without exposing their actual ports.
3. **Demonstrate Independent CRUD Operations**:
   - Call `GET http://localhost:8085/api/users` and show pre-seeded users.
   - Call `POST http://localhost:8085/api/users` to register a new user.
   - Call `GET http://localhost:8085/api/products/1/stock` showing stock = 50.
4. **Demonstrate the Complete Order Orchestration (OpenFeign Flow)**:
   - Call `POST http://localhost:8085/api/orders` with:
     ```json
     {
       "userId": 1,
       "productId": 1,
       "quantity": 2,
       "paymentMethod": "CREDIT_CARD"
     }
     ```
   - Explain the 7 internal steps:
     1. Validate User via `UserClient.getUserById(1)`.
     2. Check Product & Stock via `ProductClient.getProductById(1)`.
     3. Calculate Total ($799.99 * 2 = $1599.98).
     4. Decrement Product Stock via `ProductClient.reduceStock(1, 2)`.
     5. Call Payment Gateway via `PaymentClient.processPayment(...)`.
     6. Save Order as `CONFIRMED`.
     7. Return consolidated order response.
5. **Demonstrate Database State & Stock Reduction**:
   - Call `GET http://localhost:8085/api/products/1/stock` showing available stock has changed from 50 to 48.
   - Call `GET http://localhost:8085/api/payments/order/1` showing payment receipt was recorded.
6. **Demonstrate SAGA Compensation on Failure**:
   - Call `POST http://localhost:8085/api/orders` with `"simulatePaymentFailure": true`.
   - Show that payment fails, but inventory stock is automatically compensated (restored) without manual intervention!
7. **Demonstrate Fault Tolerance on Service Downtime**:
   - Stop `payment-service` and make an order request.
   - Show that the system returns a clean `503 Service Unavailable` error and ensures product inventory is preserved.
