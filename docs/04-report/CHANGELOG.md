# NearPick Project Changelog

All notable changes to the NearPick project will be documented in this file.

## [Phase 4.5] - 2026-02-27

### Added

- **Swagger/OpenAPI Integration** — SpringDoc OpenAPI library with JWT Bearer authentication scheme
  - SwaggerConfig bean with OpenAPI metadata (title, version, description)
  - All 7 controllers annotated with @Tag (class-level) and @Operation/@SecurityRequirement (method-level)
  - Swagger UI accessible at `/swagger-ui.html` with 24 API endpoints documented
  - LocalSwaggerSecurityConfig for environment-specific access control (local=public, prod=restricted)

- **Test Coverage Infrastructure** — Comprehensive testing framework across 3 layers
  - **Controller Tests**: 7 test files, 25+ test cases using @SpringBootTest(webEnvironment=MOCK) + MockMvcBuilders
  - **Service Tests**: 4 test files, 20+ test cases using @ExtendWith(MockitoExtension) with Mockito mocks
  - **Value Object Tests**: 4 test files, 19+ test cases with boundary value validation (Email, Password, Location, BusinessRegNo)
  - **Integration Test**: @SpringBootTest context loading verification with MySQL test database

- **JaCoCo Code Coverage Configuration** — Multi-module coverage targets
  - app: 60% (Controllers, Security, Config — Entity/Repository excluded)
  - domain: 90% (Value Objects — critical for data validation)
  - domain-nearpick: 80% (Service implementations — Entity/Repository/Mapper excluded)
  - common: Standard coverage (utility classes)

- **Spring Boot 4.x Dependency Updates**
  - spring-boot-jackson-module-kotlin (explicit Kotlin Jackson support)
  - spring-boot-flyway (separated from auto-config in Boot 4.x)
  - springdoc-openapi-starter-webmvc-ui:3.0.0 (OpenAPI documentation)

- **Database Migration Framework** (Flyway)
  - V1__init_schema.sql — Complete schema initialization for all 9 entities
  - V2__insert_dummy_data.sql — Test data insertion for integration tests
  - baseline-on-migrate=true for existing database compatibility

- **Test Environment Configuration**
  - application-test.properties with MySQL test database (nearpick_test)
  - H2 replaced with MySQL for environment parity with production
  - Flyway disabled in test profile to prevent migrations during test context load
  - JWT test secret (32+ bytes) configured

### Changed

- **Controller Testing Pattern** — Adapted to Spring Boot 4.x @WebMvcTest removal
  - From: @WebMvcTest annotation (deprecated in Boot 4.x)
  - To: @SpringBootTest(webEnvironment=MOCK) + MockMvcBuilders.webAppContextSetup + springSecurity()
  - Provides equivalent isolation while supporting Boot 4.x

- **Jackson Configuration** — Explicit Kotlin module registration
  - From: Auto-discovered KotlinModule (Boot 3.x)
  - To: Explicit spring-boot-jackson-module-kotlin dependency (Boot 4.x requirement)

- **Swagger Security Model** — Environment-aware configuration
  - From: Single SecurityConfig.permitAll() for all profiles
  - To: LocalSwaggerSecurityConfig (@Profile("local"), @Order(1)) for local environment; production restricted

### Fixed

- **Spring Boot 4.x Compatibility Issues**
  - Resolved Kotlin data class deserialization with explicit Jackson Kotlin module
  - Fixed Flyway auto-configuration by adding optional spring-boot-flyway module
  - Corrected MockMvc type inference with `.apply<DefaultMockMvcBuilder>(springSecurity())`

- **Test Database Environment**
  - Replaced H2 in-memory (MODE=MySQL) with actual MySQL test database for accurate query behavior
  - Eliminated false-positive test passes due to H2/MySQL dialect differences

### Technical Debt Addressed

- Established test patterns for all layer types (Controller, Service, VO, Integration)
- Created template-based approach for future service/controller tests (Phase 5+)
- Documented Spring Boot 4.x migration patterns for knowledge transfer
- Set baseline code coverage targets enforced by JaCoCo verification

---

## [Phase 4] - 2026-02-15

### Added

- Core API implementation: 24 endpoints across 7 controllers
  - Auth: signup (consumer, merchant), login
  - Products: nearby, detail, create, close, myProducts
  - Wishlist: toggle, list
  - Reservations: create, cancel, confirm, list, merchantList
  - Flash Purchases: purchase, list
  - Merchants: dashboard, profile
  - Admin: userList, suspend, productList, reject, withdrawUser, forceCloseProduct, getProfile

- Service layer with business logic
  - AuthServiceImpl: registration, login with password encoding
  - ProductServiceImpl: nearby search, detail retrieval, product management
  - WishlistServiceImpl: wishlist toggle and listing
  - ReservationServiceImpl: reservation lifecycle management
  - FlashPurchaseServiceImpl: flash sale with pessimistic locking
  - MerchantServiceImpl: merchant dashboard and profile
  - AdminServiceImpl: user suspension, product rejection, user withdrawal

- Value Objects with strict validation
  - Email: format validation, masking, local part extraction
  - Password: 8+ chars, alphanumeric requirement
  - Location: latitude/longitude bounds checking (-90/90, -180/180)
  - BusinessRegNo: Korean business registration number format (###-##-#####)

- MySQL Database Schema
  - 9 entities: User, ConsumerProfile, MerchantProfile, AdminProfile, Product, PopularityScore, Wishlist, Reservation, FlashPurchase
  - Location: DECIMAL(10,7) for lat/lng (PostGIS-ready)
  - PopularityScore: separate table for batch updates
  - AdminProfile.permissions: TEXT (migrate to jsonb on PostgreSQL transition)

- Security Configuration
  - JWT authentication with configurable expiration
  - SecurityConfig: role-based access control (CONSUMER, MERCHANT, ADMIN)
  - JwtTokenProvider and JwtAuthenticationFilter

---

## [Phase 3] - 2026-02-10

### Added

- Screen flow mockups: 13 screens covering consumer, merchant, and admin journeys
- API endpoint derivation: 20 core endpoints extracted from mockup flows
- UX specifications for each screen with navigation flows

---

## [Phase 2.5] - 2026-02-05

### Added

- README.md with project overview and quick start guide
- Wiki documentation (00-overview, 01-domain-glossary, 02-module-structure, 03-dev-guide)
- PULL_REQUEST_TEMPLATE.md for standardized PR descriptions
- Branch naming conventions (feature/phase-N-{name}, bugfix/, hotfix/)
- Commit message conventions (type(scope): subject)

---

## [Phase 2] - 2026-02-01

### Added

- Multi-module Gradle structure
  - app: Controllers, Spring Boot entry point, security config
  - common: ApiResponse, BusinessException, ErrorCode
  - domain: Service interfaces, enums, DTOs
  - domain-nearpick: ServiceImpl, JPA entities, repositories, mappers

- CONVENTIONS.md with comprehensive coding standards
  - Kotlin/Java style guide
  - Package structure and import ordering
  - API response format standards
  - Error handling patterns
  - Database naming conventions

- Domain enums and models
  - UserRole: CONSUMER, MERCHANT, ADMIN
  - UserStatus: ACTIVE, SUSPENDED, WITHDRAWN
  - AdminLevel: MANAGER, ADMIN, SUPER_ADMIN
  - ProductStatus: ACTIVE, CLOSED, REJECTED
  - ReservationStatus: PENDING, CONFIRMED, COMPLETED, CANCELLED
  - FlashPurchaseStatus: PENDING, COMPLETED, CANCELLED

---

## [Phase 1] - 2026-01-20

### Added

- Project initialization with Spring Boot 4.0.3, Kotlin 2.2.21, Java 17
- Domain glossary: 30+ terms (consumer, merchant, product, reservation, flash purchase, etc.)
- Database schema design: 9 entities with relationships
- API endpoint list: 24 endpoints across 7 controllers
- Development environment setup guide

---

## Future Phases

### [Phase 5] - Design System & UI Implementation

- Component library development
- API client SDK generation from Swagger
- Frontend application implementation
- UI/UX refinement based on Phase 4.5 API stability

### [Phase 6] - E2E Testing & Integration

- End-to-end test automation (Cypress/Playwright)
- API contract verification
- Performance testing

### [Phase 7] - Security Hardening

- Security audit and penetration testing
- HTTPS/TLS configuration
- Authentication/authorization review

### [Phase 8] - Review & Optimization

- Code review and refactoring
- Performance optimization
- Database query optimization

### [Phase 9] - Deployment & Production

- CI/CD pipeline setup
- Production environment configuration
- Monitoring and alerting setup
- Production deployment and go-live

---

## Project Statistics

| Phase | Deliverables | Test Coverage | Key Focus |
|-------|:-------------:|:-------------:|-----------|
| Phase 1 | Schema, Glossary, Endpoints | N/A | Domain definition |
| Phase 2 | Architecture, Conventions | N/A | Technical foundation |
| Phase 2.5 | Docs, Wiki, PR Template | N/A | Development workflow |
| Phase 3 | Mockups, Flows | N/A | UX design |
| Phase 4 | 24 APIs, 7 Controllers, Services | 0% | Backend implementation |
| Phase 4.5 | Swagger, Tests, Coverage | 70-90% | API quality & documentation |
| Phase 5+ | UI, SDK, E2E, Security, Deploy | TBD | Frontend & production |

---

**Last Updated**: 2026-02-27
**Project Level**: Enterprise (bkit PDCA pipeline)
**Current Phase**: Phase 4.5 Complete → Phase 5 Ready
