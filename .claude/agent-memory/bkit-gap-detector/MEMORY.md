# Gap Detector Memory - NearPick

## Phase 4.5 Analysis (2026-02-27)
- **Match Rate**: 97.5% (122 design items, 119 matched, 3 missing)
- **Missing**: AuthControllerTest signupMerchant, ProductControllerTest close, FlashPurchaseServiceImplTest getMyPurchases
- **12 enhancements** beyond design (extra test scenarios, Flyway disable, LocalSwaggerSecurityConfig)
- **3 intentional deviations**: H2->MySQL, @WebMvcTest->@SpringBootTest, Swagger security via @Profile("local")

## Spring Boot 4.x Test Patterns (confirmed)
- Controller tests: @SpringBootTest(webEnvironment=MOCK) + MockMvcBuilders.webAppContextSetup + springSecurity()
- Service tests: @ExtendWith(MockitoExtension::class) + @Mock/@InjectMocks (no Spring context)
- Auth in tests: UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_X")))
- @MockitoBean replaces @MockBean in Spring Boot 4.x

## Project Test Structure
- Controller tests: app/src/test/.../controller/
- Service tests: domain-nearpick/src/test/.../{feature}/service/
- VO tests: domain/src/test/.../model/
- Integration: app/src/test/.../NearPickApplicationTests.kt
- Test DB: MySQL nearpick_test (not H2)
