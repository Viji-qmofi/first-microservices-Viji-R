# Repo Structure Summary

Repo: **first-microservices-Viji-R** — a beginner Spring Boot microservices demo (E-Commerce app), plus a Dockerized dev environment for Claude Code / OpenCode.

## Top-level layout

```
.
├── Dockerfile                 → dev container image (Maven + Java 21 + Claude Code/OpenCode/ngrok)
├── docker-entrypoint.sh        → restores/persists Claude Code credentials across container runs
├── settings.json, statusline.sh→ Claude Code config copied into the dev image
├── README.md                   → full project documentation (architecture, endpoints, setup)
├── .metadata/, .project        → Eclipse workspace metadata (IDE-generated, not app code)
├── ecom-eureka-registry/       → service discovery server
├── ecom-api-gateway/           → API gateway
├── ecom-product-service/       → product catalog microservice
└── ecom-order-service/         → order management microservice
```

Each service module is an independent Maven project (own `pom.xml`, `mvnw`, `src/main`, `src/test`), built on **Spring Boot 4.0.5** with **Spring Cloud** (dependency versions managed via `spring-cloud-dependencies` BOM), targeting **Java 21**.

## Services

| Service | Eureka name | Port | Role |
|---|---|---|---|
| `ecom-eureka-registry` | _(server)_ | 8761 | Netflix Eureka service registry |
| `ecom-api-gateway` | `api-gateway` | 9000 | Spring Cloud Gateway (WebMVC) — routes `/catalog/**` → product-service, `/purchase/**` → order-service |
| `ecom-product-service` | `product-service` | 8081 | Product catalog CRUD/browse |
| `ecom-order-service` | `order-service` | 8082 | Order placement; calls product-service via Feign |

## Module source structure

**ecom-eureka-registry** (`com.ecom`)
- `EcomEurekaRegistryApplication.java` — Eureka server bootstrap
- Key dependency: `spring-cloud-starter-netflix-eureka-server`

**ecom-api-gateway** (`com.apigateway`)
- `EcomApiGatewayApplication.java` — gateway bootstrap
- Key dependencies: `spring-cloud-starter-gateway-server-webmvc`, `spring-cloud-starter-netflix-eureka-client`, actuator
- Routing/header-rewrite rules defined in `application.yml`

**ecom-product-service** (`com.productcatalog`)
- `controllers/ProductController.java`
- `service/IProductService.java`, `service/ProductServiceImpl.java`
- `model/Product.java`
- `util/ProductRepo.java` (in-memory data store)

**ecom-order-service** (`com.productorder`)
- `controllers/OrderController.java`
- `service/IOrderService.java`, `service/OrderServiceImpl.java`
- `model/Product.java`
- `feign/IProductServiceFeignClient.java` — declarative Feign client calling `product-service` by Eureka name
- Key dependencies: `spring-cloud-starter-openfeign`, `lombok`

Each module also has a matching `*ApplicationTests.java` smoke test under `src/test`.

## Architecture flow

```
Client → ecom-api-gateway (9000)
           ├─ /catalog/**  → lb://product-service (rewritten to /catalog-service/v1/**)
           └─ /purchase/** → lb://order-service   (rewritten to /order-service/v1/orders/**)

All services register with ecom-eureka-registry (8761).
ecom-order-service → [FeignClient] → ecom-product-service (load-balanced via Eureka)
```

## Non-application files

- `.metadata/` — a large tree of Eclipse workspace/IDE history files (hundreds of tracked entries under `.plugins/org.eclipse.*`); not part of the application, safe to ignore when reasoning about the codebase.
- `Dockerfile` / `docker-entrypoint.sh` / `settings.json` / `statusline.sh` — support a containerized dev environment for AI coding agents (Claude Code, OpenCode), unrelated to the microservices' runtime behavior.
