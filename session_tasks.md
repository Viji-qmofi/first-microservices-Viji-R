# Parallel Agent Session Tasks

## Session A

Branch name: feature/agent-a
Worktree directory: ../target-agent-a
Task: Add or improve unit tests for ecom-product-service's controller and service layer.
Files or folders the agent may write to: ecom-product-service/src/test/**
Files or folders the agent may read but not write to: ecom-product-service/src/main/**, ecom-product-service/pom.xml
Commands the agent may run: ./mvnw test (run from inside ecom-product-service)
Definition of done: New or updated tests compile and pass via `./mvnw test`, covering the currently-untested paths in the product-service controller/service classes, with no changes outside `ecom-product-service/src/test/`.

## Session B

Branch name: feature/agent-b
Worktree directory: ../target-agent-b
Task: Write a short developer note explaining how service discovery and request routing work across ecom-eureka-registry and ecom-api-gateway.
Files or folders the agent may write to: docs/service-discovery-and-routing.md (new file)
Files or folders the agent may read but not write to: ecom-eureka-registry/**, ecom-api-gateway/**, README.md
Commands the agent may run: none required (read-only inspection of source files — no build or test commands needed for a docs task)
Definition of done: A single new markdown file at docs/service-discovery-and-routing.md accurately describing how the services register with Eureka and how the gateway routes to product-service and order-service (including the `lb://` load-balancing), with no changes to any file outside that new doc.
