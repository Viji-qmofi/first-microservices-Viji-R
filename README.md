# 🛒 E-Commerce Microservices Application

A beginner-friendly microservices project built with **Spring Boot**, **Spring Cloud Gateway**, **Netflix Eureka**, and **OpenFeign**. Four independent services work together — a gateway routes all traffic, Eureka handles service discovery, and the order service calls the product service via a Feign client.

---

## 🏗️ Architecture Overview

```
Client
  │
  ▼
ecom-api-gateway  (port 9000)
  │  ├─ /catalog/**   ──► ecom-product-service  (lb://product-service)
  │  └─ /purchase/**  ──► ecom-order-service    (lb://order-service)
  │
  └─ All services register with:
       ecom-eureka-registry  (port 8761)

Inter-service communication:
  ecom-order-service ──[FeignClient]──► ecom-product-service
```

---

## 📦 Services

| Service | Eureka Name | Port | Description |
|---|---|---|---|
| `ecom-eureka-registry` | _(server)_ | `8761` | Netflix Eureka Server — service registration & discovery |
| `ecom-api-gateway` | `api-gateway` | `9000` | Spring Cloud Gateway — routes & filters all client requests |
| `ecom-product-service` | `product-service` | `8081` | Product catalog — browse and filter products |
| `ecom-order-service` | `order-service` | `8082` | Order management — calls product service via FeignClient |

---

## 🛠️ Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring Cloud Netflix Eureka** — Service discovery
- **Spring Cloud Gateway (WebMVC)** — API Gateway & routing
- **OpenFeign** — Declarative HTTP client for inter-service calls
- **Maven** — Build tool

---

## 📁 Project Structure

```
microservices/
├── ecom-eureka-registry/
│   └── src/main/
│       ├── java/   → EcomEurekaRegistryApplication.java
│       └── resources/application.yml
│
├── ecom-api-gateway/
│   └── src/main/
│       ├── java/   → EcomApiGatewayApplication.java
│       └── resources/application.yml
│
├── ecom-product-service/
│   └── src/main/java/com/productcatalog/
│       ├── controllers/  → ProductController.java
│       ├── model/
│       ├── service/      → IProductService.java, ProductServiceImpl.java
│       └── util/
│
└── ecom-order-service/
    └── src/main/java/com/productorder/
        ├── controllers/  → OrderController.java
        ├── feign/        → IProductServiceFeignClient.java
        ├── model/        → Product.java
        └── service/      → IOrderService.java, OrderServiceImpl.java
```

---

## ⚙️ Configuration (application.yml)

### Eureka Registry — `ecom-eureka-registry`

```yaml
server:
  port: 8761

spring:
  application:
    name: ecom-eureka-registry

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

### API Gateway — `ecom-api-gateway`

```yaml
server:
  port: 9000

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            # Product Service Route
            - id: productService
              uri: lb://product-service
              predicates:
                - Path=/catalog/**
              filters:
                - AddRequestHeader=X-catalog-header, catalogofproducts
                - AddResponseHeader=X-catalog-responsetime, "#{T(java.time.LocalDate).now()}"
                - RewritePath=/catalog/(?<segment>.*), /catalog-service/v1/${segment}

            # Order Service Route
            - id: orderService
              uri: lb://order-service
              predicates:
                - Path=/purchase/**
              filters:
                - AddRequestHeader=X-order-header, orderingitems
                - AddResponseHeader=X-order-responsetime, "#{T(java.time.LocalDate).now()}"
                - RewritePath=/purchase/(?<segment>.*), /order-service/v1/orders/${segment}

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Product Service — `ecom-product-service`

```yaml
server:
  port: 8081

spring:
  application:
    name: product-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

### Order Service — `ecom-order-service`

```yaml
server:
  port: 8082

spring:
  application:
    name: order-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

---

## 🔌 API Endpoints

All requests go through the API Gateway at `http://localhost:9000`. The gateway rewrites paths before forwarding to each service.

### Product Service

| Method | Gateway URL | Forwarded To (Internal) |
|---|---|---|
| `GET` | `/catalog/products` | `/catalog-service/v1/products` |
| `GET` | `/catalog/products/productId/{id}` | `/catalog-service/v1/products/productId/{id}` |
| `GET` | `/catalog/products/category/{cat}` | `/catalog-service/v1/products/category/{cat}` |

### Order Service

| Method | Gateway URL | Forwarded To (Internal) |
|---|---|---|
| `POST` | `/purchase/place-order/{id}` | `/order-service/v1/orders/place-order/{id}` |
| `GET` | `/purchase/orders` | `/order-service/v1/orders` |

> **Gateway Filters Applied on Every Request:**
> - A custom **request header** is injected (e.g. `X-catalog-header: catalogofproducts`)
> - A custom **response header** with today's date is added (e.g. `X-catalog-responsetime: 2026-05-09`)

---

## 🤝 Inter-Service Communication (FeignClient)

The **Order Service** calls the **Product Service** using OpenFeign — no hardcoded URLs. Feign resolves `product-service` automatically via Eureka.

```java
// ecom-order-service: com.productorder.feign.IProductServiceFeignClient
@FeignClient(name = "product-service")
public interface IProductServiceFeignClient {

    @GetMapping("/catalog-service/v1/products/productId/{id}")
    Product getProductById(@PathVariable int id);
}
```

When an order is placed, the order service fetches product details from the product service using Eureka-based load balancing — no manual IP or port needed.

---

## 🚀 Getting Started

### Prerequisites

- [Java 21](https://adoptium.net/)
- [Maven](https://maven.apache.org/)
- [Git](https://git-scm.com/)

### 1. Clone the Repository

```bash
git clone https://github.com/Viji-qmofi/first-microservices-Viji-R.git
cd first-microservices-Viji-R
```

### 2. Start Services in Order

> ⚠️ **Always start in this exact order.** Other services depend on Eureka being available first.

```bash
# Step 1 — Eureka Service Registry
cd ecom-eureka-registry
mvn spring-boot:run
# Confirm: http://localhost:8761

# Step 2 — Product Service
cd ../ecom-product-service
mvn spring-boot:run

# Step 3 — Order Service
cd ../ecom-order-service
mvn spring-boot:run

# Step 4 — API Gateway
cd ../ecom-api-gateway
mvn spring-boot:run
```

### 3. Verify Everything is Running

Open [http://localhost:8761](http://localhost:8761) — the Eureka dashboard should show `api-gateway`, `product-service`, and `order-service` all registered as **UP**.

### 4. Test the Endpoints

```bash
# Get all products via the gateway
curl http://localhost:9000/catalog/products

# Get a product by ID
curl http://localhost:9000/catalog/products/productId/1

# Place an order via the gateway
curl -X POST http://localhost:9000/purchase/place-order/1
```

---

## 📚 Key Concepts Demonstrated

- **Service Discovery** — Services register with Eureka by name. No hardcoded IPs between services.
- **API Gateway Pattern** — Single entry point at port 9000 routes and filters all traffic.
- **Path Rewriting** — Gateway rewrites public paths (e.g. `/catalog/**`) to internal paths (e.g. `/catalog-service/v1/`).
- **Custom Headers** — Gateway injects request/response headers (custom tags, timestamps) transparently.
- **FeignClient** — Declarative REST client lets the order service call the product service with a clean Java interface.
- **Load Balancing** — `lb://` URIs enable client-side load balancing via Spring Cloud LoadBalancer.

---

## 🧑‍💻 Author

**Viji R** — [@Viji-qmofi](https://github.com/Viji-qmofi)

*First microservices project — exploring Spring Cloud, Eureka, Gateway, and OpenFeign.*
