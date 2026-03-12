# Design: phase11-product-enhancement

> Phase 11 — 상품 고도화 (이미지, 카테고리, 메뉴 옵션)

## 개요

| 항목 | 내용 |
|------|------|
| **Feature** | phase11-product-enhancement |
| **Phase** | 11 |
| **목표** | 상품 이미지 업로드 + 카테고리 체계 + 음식 메뉴 옵션 + 비음식 스펙 속성 |
| **작성일** | 2026-03-12 |
| **브랜치** | `feature/phase11-product-enhancement` |
| **참조 Plan** | `docs/01-plan/features/phase11-product-enhancement.plan.md` |

---

## 1. API 명세

### 1.1 상품 생성 수정 — category, specs 추가

```
POST /products
Authorization: Bearer {JWT}  (MERCHANT role)
```

**Request Body 변경**
```json
{
  "title": "아메리카노",
  "description": "당일 원두 직접 로스팅",
  "price": 3500,
  "productType": "FLASH_SALE",
  "stock": 50,
  "availableFrom": "2026-03-12T09:00:00",
  "availableUntil": "2026-03-12T21:00:00",
  "category": "FOOD",
  "specs": null
}
```

`category` 값: `FOOD | BEVERAGE | BEAUTY | DAILY | OTHER` (nullable, 미지정 시 null)
`specs` 값: `BEAUTY | DAILY | OTHER` 카테고리용 JSON 배열 — `FOOD | BEVERAGE`는 null 권장

---

### 1.2 상품 상세 조회 — images, menuOptions, specs, category 포함

```
GET /products/{id}
Authorization: Bearer {JWT}  (optional)
```

**Response 200 — ProductDetailResponse (변경)**
```json
{
  "id": 1,
  "title": "아메리카노",
  "description": "당일 원두 직접 로스팅",
  "price": 3500,
  "productType": "FLASH_SALE",
  "status": "ACTIVE",
  "stock": 50,
  "category": "FOOD",
  "availableFrom": "2026-03-12T09:00:00",
  "availableUntil": "2026-03-12T21:00:00",
  "shopLat": 37.5665000,
  "shopLng": 126.9780000,
  "shopAddress": "서울 중구 명동",
  "merchantName": "스타벅스 명동점",
  "wishlistCount": 15,
  "reservationCount": 3,
  "purchaseCount": 42,
  "images": [
    {"id": 1, "url": "https://cdn.example.com/products/1/images/abc.jpg", "displayOrder": 0}
  ],
  "menuOptions": [
    {
      "id": 1,
      "name": "사이즈",
      "required": true,
      "maxSelect": 1,
      "displayOrder": 0,
      "choices": [
        {"id": 1, "name": "Small", "additionalPrice": 0, "displayOrder": 0},
        {"id": 2, "name": "Large", "additionalPrice": 500, "displayOrder": 1}
      ]
    }
  ],
  "specs": null
}
```

비음식 상품(BEAUTY) 예시 — `menuOptions: []`, `specs: [{"key":"용량","value":"200ml"}]`

---

### 1.3 상품 nearby 조회 — category 필터 추가

```
GET /products/nearby?lat=37.5&lng=127.0&radius=3.0&category=FOOD
```

`category` 파라미터는 optional. 미전달 시 전체 카테고리 반환.

---

### 1.4 Presigned URL 발급

```
POST /products/{id}/images/presigned
Authorization: Bearer {JWT}  (MERCHANT — 본인 상품만)
```

**Request Body**
```json
{
  "filename": "coffee.jpg",
  "contentType": "image/jpeg"
}
```

**Response 200**
```json
{
  "presignedUrl": "https://near-pick-bucket.s3.ap-northeast-2.amazonaws.com/products/1/images/uuid.jpg?X-Amz-...",
  "s3Key": "products/1/images/550e8400-e29b-41d4-a716-446655440000.jpg",
  "expiresInSeconds": 300
}
```

**Error**
- `400 PRODUCT_IMAGE_LIMIT_EXCEEDED` — 이미 5장 등록됨
- `400 INVALID_IMAGE_TYPE` — 허용되지 않는 확장자 (허용: jpg, jpeg, png, webp)
- `403 FORBIDDEN` — 타인 상품

**Local mock mode** (`product.image.upload.enabled=false`):
```json
{
  "presignedUrl": "http://localhost:8080/mock-upload/products/1/images/uuid.jpg",
  "s3Key": "products/1/images/mock-uuid.jpg",
  "expiresInSeconds": 300
}
```

---

### 1.5 이미지 URL 저장 (업로드 완료 후)

```
POST /products/{id}/images
Authorization: Bearer {JWT}  (MERCHANT — 본인 상품만)
```

**Request Body**
```json
{
  "s3Key": "products/1/images/550e8400-e29b-41d4-a716-446655440000.jpg",
  "displayOrder": 0
}
```

**Response 201 — ProductImageResponse**
```json
{
  "id": 1,
  "url": "https://near-pick-bucket.s3.ap-northeast-2.amazonaws.com/products/1/images/550e8400-e29b-41d4-a716-446655440000.jpg",
  "s3Key": "products/1/images/550e8400-e29b-41d4-a716-446655440000.jpg",
  "displayOrder": 0
}
```

---

### 1.6 이미지 삭제

```
DELETE /products/{id}/images/{imageId}
Authorization: Bearer {JWT}  (MERCHANT — 본인 상품만)
```

**Response**
- `204 No Content`
- `404 PRODUCT_IMAGE_NOT_FOUND`

S3 오브젝트도 함께 삭제 (DeleteObjectRequest).
Local mock mode: S3 삭제 skip, DB만 삭제.

---

### 1.7 이미지 순서 변경

```
PUT /products/{id}/images/order
Authorization: Bearer {JWT}  (MERCHANT — 본인 상품만)
```

**Request Body**
```json
[
  {"imageId": 2, "displayOrder": 0},
  {"imageId": 1, "displayOrder": 1}
]
```

**Response 200 — `List<ProductImageResponse>`**

---

### 1.8 메뉴 옵션 그룹 등록 (일괄)

```
POST /products/{id}/menu-options
Authorization: Bearer {JWT}  (MERCHANT — 본인 상품만, FOOD/BEVERAGE 카테고리만)
```

**Request Body**
```json
[
  {
    "name": "사이즈",
    "required": true,
    "maxSelect": 1,
    "displayOrder": 0,
    "choices": [
      {"name": "Small", "additionalPrice": 0, "displayOrder": 0},
      {"name": "Large", "additionalPrice": 500, "displayOrder": 1}
    ]
  }
]
```

**Response 201 — `List<ProductMenuOptionGroupResponse>`**

**Error**
- `400 MENU_OPTION_NOT_ALLOWED` — FOOD/BEVERAGE 이외 카테고리

---

### 1.9 메뉴 옵션 그룹 삭제

```
DELETE /products/{id}/menu-options/{groupId}
Authorization: Bearer {JWT}  (MERCHANT — 본인 상품만)
```

**Response 204 No Content**

---

## 2. DTO 설계

### 2.1 ProductCategory enum (신규)

**파일**: `domain/src/main/kotlin/com/nearpick/domain/product/ProductCategory.kt`

```kotlin
package com.nearpick.domain.product

enum class ProductCategory {
    FOOD,      // 음식
    BEVERAGE,  // 음료
    BEAUTY,    // 뷰티
    DAILY,     // 생활용품
    OTHER,     // 기타
}
```

---

### 2.2 상품 이미지 DTOs (신규)

**파일**: `domain/src/main/kotlin/com/nearpick/domain/product/dto/ProductImageDtos.kt`

```kotlin
package com.nearpick.domain.product.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class PresignedUrlRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val filename: String,

    @field:NotBlank
    val contentType: String,
)

data class PresignedUrlResponse(
    val presignedUrl: String,
    val s3Key: String,
    val expiresInSeconds: Int,
)

data class ProductImageSaveRequest(
    @field:NotBlank val s3Key: String,
    val displayOrder: Int = 0,
)

data class ProductImageResponse(
    val id: Long,
    val url: String,
    val s3Key: String,
    val displayOrder: Int,
)

data class ImageOrderItem(
    val imageId: Long,
    val displayOrder: Int,
)
```

---

### 2.3 메뉴 옵션 DTOs (신규)

**파일**: `domain/src/main/kotlin/com/nearpick/domain/product/dto/ProductMenuOptionDtos.kt`

```kotlin
package com.nearpick.domain.product.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MenuChoiceRequest(
    @field:NotBlank @field:Size(max = 50) val name: String,
    @field:Min(0) val additionalPrice: Int = 0,
    val displayOrder: Int = 0,
)

data class MenuOptionGroupRequest(
    @field:NotBlank @field:Size(max = 50) val name: String,
    val required: Boolean = false,
    @field:Min(1) val maxSelect: Int = 1,
    val displayOrder: Int = 0,
    val choices: List<MenuChoiceRequest> = emptyList(),
)

data class MenuChoiceResponse(
    val id: Long,
    val name: String,
    val additionalPrice: Int,
    val displayOrder: Int,
)

data class ProductMenuOptionGroupResponse(
    val id: Long,
    val name: String,
    val required: Boolean,
    val maxSelect: Int,
    val displayOrder: Int,
    val choices: List<MenuChoiceResponse>,
)
```

---

### 2.4 ProductDtos.kt 변경

**파일**: `domain/src/main/kotlin/com/nearpick/domain/product/dto/ProductDtos.kt`

`ProductCreateRequest` 변경:
```kotlin
data class ProductCreateRequest(
    @field:NotBlank val title: String,
    val description: String? = null,
    @field:Min(0) val price: Int,
    @field:NotNull val productType: ProductType,
    @field:Min(0) val stock: Int = 0,
    val availableFrom: LocalDateTime? = null,
    val availableUntil: LocalDateTime? = null,
    val category: ProductCategory? = null,            // 신규
    val specs: List<ProductSpecItem>? = null,          // 신규
) { ... }

data class ProductSpecItem(                            // 신규 — 파일 내 추가
    @field:NotBlank val key: String,
    @field:NotBlank val value: String,
)
```

`ProductDetailResponse` 변경 (추가 필드):
```kotlin
data class ProductDetailResponse(
    // 기존 필드 유지
    val id: Long,
    val title: String,
    val description: String?,
    val price: Int,
    val productType: ProductType,
    val status: ProductStatus,
    val stock: Int,
    val availableFrom: LocalDateTime?,
    val availableUntil: LocalDateTime?,
    val shopLat: BigDecimal,
    val shopLng: BigDecimal,
    val shopAddress: String?,
    val merchantName: String,
    val wishlistCount: Long,
    val reservationCount: Long,
    val purchaseCount: Long,
    // 신규 필드
    val category: ProductCategory?,
    val images: List<ProductImageResponse>,
    val menuOptions: List<ProductMenuOptionGroupResponse>,
    val specs: List<ProductSpecItem>?,
) : java.io.Serializable
```

`ProductSummaryResponse` 변경 (category 추가):
```kotlin
data class ProductSummaryResponse(
    // 기존 필드 유지
    val id: Long,
    val title: String,
    val price: Int,
    val productType: ProductType,
    val status: ProductStatus,
    val popularityScore: Double,
    val distanceKm: Double,
    val merchantName: String,
    val shopAddress: String?,
    val shopLat: BigDecimal,
    val shopLng: BigDecimal,
    // 신규
    val category: ProductCategory?,
    val thumbnailUrl: String?,    // images[0].url (없으면 null)
) : java.io.Serializable
```

`ProductNearbyRequest` 변경 (category 필터 추가):
```kotlin
data class ProductNearbyRequest(
    // 기존 유지
    val lat: BigDecimal? = null,
    val lng: BigDecimal? = null,
    @field:Positive @field:Max(50) val radius: Double = 5.0,
    val sort: SortType = SortType.POPULARITY,
    @field:Min(0) val page: Int = 0,
    @field:Positive @field:Max(100) val size: Int = 20,
    val locationSource: LocationSource = LocationSource.DIRECT,
    val savedLocationId: Long? = null,
    // 신규
    val category: ProductCategory? = null,
) { ... }
```

---

### 2.5 서비스 인터페이스 (신규)

**파일**: `domain/src/main/kotlin/com/nearpick/domain/product/ProductImageService.kt`

```kotlin
package com.nearpick.domain.product

import com.nearpick.domain.product.dto.*

interface ProductImageService {
    fun generatePresignedUrl(merchantId: Long, productId: Long, request: PresignedUrlRequest): PresignedUrlResponse
    fun saveImageUrl(merchantId: Long, productId: Long, request: ProductImageSaveRequest): ProductImageResponse
    fun deleteImage(merchantId: Long, productId: Long, imageId: Long)
    fun reorderImages(merchantId: Long, productId: Long, orders: List<ImageOrderItem>): List<ProductImageResponse>
    fun getImages(productId: Long): List<ProductImageResponse>
}
```

**파일**: `domain/src/main/kotlin/com/nearpick/domain/product/ProductMenuOptionService.kt`

```kotlin
package com.nearpick.domain.product

import com.nearpick.domain.product.dto.*

interface ProductMenuOptionService {
    fun saveMenuOptions(merchantId: Long, productId: Long, groups: List<MenuOptionGroupRequest>): List<ProductMenuOptionGroupResponse>
    fun deleteMenuOptionGroup(merchantId: Long, productId: Long, groupId: Long)
    fun getMenuOptions(productId: Long): List<ProductMenuOptionGroupResponse>
}
```

---

## 3. 엔티티 설계

### 3.1 ProductEntity 변경

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/entity/ProductEntity.kt`

추가 필드:
```kotlin
@Enumerated(EnumType.STRING)
@Column(name = "category", length = 20)
var category: ProductCategory? = null

@Column(name = "specs", columnDefinition = "TEXT")
var specs: String? = null  // ObjectMapper로 List<ProductSpecItem> 직렬화
```

---

### 3.2 ProductImageEntity (신규)

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/entity/ProductImageEntity.kt`

```kotlin
package com.nearpick.nearpick.product.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "product_images",
    indexes = [Index(name = "idx_product_images_product_id", columnList = "product_id")]
)
class ProductImageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: ProductEntity,

    @Column(name = "s3_key", nullable = false, length = 500)
    val s3Key: String,

    @Column(nullable = false, length = 1000)
    val url: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
```

---

### 3.3 ProductMenuOptionGroupEntity (신규)

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/entity/ProductMenuOptionGroupEntity.kt`

```kotlin
package com.nearpick.nearpick.product.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "product_menu_option_groups",
    indexes = [Index(name = "idx_menu_option_groups_product_id", columnList = "product_id")]
)
class ProductMenuOptionGroupEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: ProductEntity,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = false)
    var required: Boolean = false,

    @Column(name = "max_select", nullable = false)
    var maxSelect: Int = 1,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val choices: MutableList<ProductMenuChoiceEntity> = mutableListOf(),
)
```

---

### 3.4 ProductMenuChoiceEntity (신규)

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/entity/ProductMenuChoiceEntity.kt`

```kotlin
package com.nearpick.nearpick.product.entity

import jakarta.persistence.*

@Entity
@Table(name = "product_menu_choices")
class ProductMenuChoiceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: ProductMenuOptionGroupEntity,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(name = "additional_price", nullable = false)
    var additionalPrice: Int = 0,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,
)
```

---

## 4. Repository 설계

### 4.1 ProductImageRepository (신규)

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/repository/ProductImageRepository.kt`

```kotlin
interface ProductImageRepository : JpaRepository<ProductImageEntity, Long> {
    fun findAllByProductIdOrderByDisplayOrder(productId: Long): List<ProductImageEntity>
    fun countByProductId(productId: Long): Long
    fun findByIdAndProductId(id: Long, productId: Long): ProductImageEntity?
}
```

---

### 4.2 ProductMenuOptionGroupRepository (신규)

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/repository/ProductMenuOptionGroupRepository.kt`

```kotlin
interface ProductMenuOptionGroupRepository : JpaRepository<ProductMenuOptionGroupEntity, Long> {
    fun findAllByProductIdOrderByDisplayOrder(productId: Long): List<ProductMenuOptionGroupEntity>
    fun deleteAllByProductId(productId: Long)
    fun findByIdAndProductId(id: Long, productId: Long): ProductMenuOptionGroupEntity?
}
```

---

### 4.3 ProductRepository 변경 — category 필터 추가

`findNearby` native query에 카테고리 조건 추가:

```sql
-- WHERE 절 추가
AND (:category IS NULL OR p.category = :category)
```

`findAllByOptionalStatus` JPQL 변경:
```kotlin
@Query("""
    SELECT p FROM ProductEntity p
    WHERE (:status IS NULL OR p.status = :status)
      AND (:category IS NULL OR p.category = :category)
    ORDER BY p.createdAt DESC
""")
fun findAllByOptionalStatus(
    @Param("status") status: ProductStatus?,
    @Param("category") category: ProductCategory?,
    pageable: Pageable
): Page<ProductEntity>
```

---

## 5. 서비스 구현 설계

### 5.1 S3Service (신규 — domain-nearpick)

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/client/S3Service.kt`

```kotlin
@Service
class S3Service(
    @Value("\${aws.s3.bucket:}") private val bucket: String,
    @Value("\${aws.s3.region:ap-northeast-2}") private val region: String,
    @Value("\${product.image.upload.enabled:false}") private val uploadEnabled: Boolean,
    private val s3Presigner: S3Presigner?,   // null when uploadEnabled=false
    private val s3Client: S3Client?,          // null when uploadEnabled=false
) {
    fun generatePresignedPutUrl(s3Key: String, contentType: String): String {
        if (!uploadEnabled) return "http://localhost:8080/mock-upload/$s3Key"
        // PutObjectPresignRequest — 5분 TTL, ContentType 조건 포함
    }

    fun buildPublicUrl(s3Key: String): String {
        if (!uploadEnabled) return "http://localhost:8080/mock-download/$s3Key"
        return "https://$bucket.s3.$region.amazonaws.com/$s3Key"
    }

    fun deleteObject(s3Key: String) {
        if (!uploadEnabled) return
        // DeleteObjectRequest
    }
}
```

---

### 5.2 ProductImageServiceImpl (신규)

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/service/ProductImageServiceImpl.kt`

핵심 로직:
```
generatePresignedUrl:
  1. 상품 조회 + 소유권 확인 (merchantId)
  2. 현재 이미지 수 확인 → 5장 이상이면 PRODUCT_IMAGE_LIMIT_EXCEEDED
  3. 확장자 검증: jpg|jpeg|png|webp 이외 → INVALID_IMAGE_TYPE
  4. s3Key = "products/{productId}/images/{UUID}.{ext}"
  5. s3Service.generatePresignedPutUrl(s3Key, contentType) → presignedUrl
  6. PresignedUrlResponse 반환 (expiresInSeconds = 300)

saveImageUrl:
  1. 상품 조회 + 소유권 확인
  2. 현재 이미지 수 재확인 (동시 요청 방어)
  3. s3Key 유효성: "products/{productId}/images/" 접두사 검증
  4. url = s3Service.buildPublicUrl(s3Key)
  5. ProductImageEntity 저장 → ProductImageResponse 반환

deleteImage:
  1. 이미지 조회 (productId + imageId) → PRODUCT_IMAGE_NOT_FOUND
  2. 상품 소유권 확인
  3. s3Service.deleteObject(image.s3Key)
  4. DB 삭제

reorderImages:
  1. 상품 소유권 확인
  2. 각 imageId에 대해 displayOrder 업데이트
  3. 전체 이미지 목록 반환
```

---

### 5.3 ProductMenuOptionServiceImpl (신규)

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/service/ProductMenuOptionServiceImpl.kt`

핵심 로직:
```
saveMenuOptions:
  1. 상품 조회 + 소유권 확인
  2. category 검증: FOOD | BEVERAGE 이외 → MENU_OPTION_NOT_ALLOWED
  3. 기존 그룹 전체 삭제 (deleteAllByProductId) — upsert 대신 전체 교체
  4. 신규 그룹 + 선택지 일괄 저장
  5. 저장된 그룹 목록 반환

deleteMenuOptionGroup:
  1. groupId + productId로 조회 → not found 시 404
  2. 상품 소유권 확인
  3. 그룹 삭제 (choices는 CascadeType.ALL로 자동 삭제)
```

---

### 5.4 ProductServiceImpl 변경

**파일**: `domain-nearpick/src/main/kotlin/com/nearpick/nearpick/product/service/ProductServiceImpl.kt`

`getNearby()` 변경:
```kotlin
override fun getNearby(request: ProductNearbyRequest, userId: Long?): Page<ProductSummaryResponse> {
    val (lat, lng) = resolveLocation(request, userId)
    val pageable = PageRequest.of(request.page, request.size)
    return productRepository.findNearby(
        lat = lat.toDouble(),
        lng = lng.toDouble(),
        radius = request.radius,
        sort = request.sort.name.lowercase(),
        category = request.category?.name,    // 신규
        pageable = pageable,
    ).map { it.toSummaryResponse() }
}
```

`getDetail()` 변경:
```kotlin
override fun getDetail(productId: Long): ProductDetailResponse {
    val product = productRepository.findById(productId)
        .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
    productRepository.incrementViewCount(productId)

    val images = productImageService.getImages(productId)
    val menuOptions = productMenuOptionService.getMenuOptions(productId)
    val specs = product.specs?.let { objectMapper.readValue<List<ProductSpecItem>>(it) }

    return product.toDetailResponse(
        wishlistCount = wishlistRepository.countByProductId(productId),
        reservationCount = reservationRepository.countByProductId(productId),
        purchaseCount = flashPurchaseRepository.countByProductId(productId),
        images = images,
        menuOptions = menuOptions,
        specs = specs,
    )
}
```

`create()` 변경:
```kotlin
// ProductEntity 생성 시 category, specs 추가
val specsJson = request.specs?.let { objectMapper.writeValueAsString(it) }
val entity = ProductEntity(
    merchant = merchant,
    title = request.title,
    description = request.description,
    price = request.price,
    productType = request.productType,
    stock = request.stock,
    availableFrom = request.availableFrom,
    availableUntil = request.availableUntil,
    shopLat = merchant.shopLat ?: ...,
    shopLng = merchant.shopLng ?: ...,
    category = request.category,
    specs = specsJson,
)
```

Redis 캐시 무효화: `@CacheEvict(value = ["products-detail"], key = "#productId")` — 이미지/메뉴 변경 시 적용.

---

## 6. 컨트롤러 설계

### 6.1 ProductImageController (신규)

**파일**: `app/src/main/kotlin/com/nearpick/app/controller/ProductImageController.kt`

```kotlin
@RestController
@RequestMapping("/products/{productId}/images")
@Tag(name = "Product Images", description = "상품 이미지 관리 API")
class ProductImageController(
    private val productImageService: ProductImageService,
) {
    @PostMapping("/presigned")
    @PreAuthorize("hasRole('MERCHANT')")
    fun generatePresignedUrl(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody request: PresignedUrlRequest,
    ): ResponseEntity<ApiResponse<PresignedUrlResponse>>

    @PostMapping
    @PreAuthorize("hasRole('MERCHANT')")
    fun saveImage(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody request: ProductImageSaveRequest,
    ): ResponseEntity<ApiResponse<ProductImageResponse>>

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('MERCHANT')")
    fun deleteImage(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @PathVariable imageId: Long,
    ): ResponseEntity<Void>

    @PutMapping("/order")
    @PreAuthorize("hasRole('MERCHANT')")
    fun reorderImages(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody orders: List<ImageOrderItem>,
    ): ResponseEntity<ApiResponse<List<ProductImageResponse>>>
}
```

---

### 6.2 ProductMenuOptionController (신규)

**파일**: `app/src/main/kotlin/com/nearpick/app/controller/ProductMenuOptionController.kt`

```kotlin
@RestController
@RequestMapping("/products/{productId}/menu-options")
@Tag(name = "Product Menu Options", description = "음식 상품 메뉴 옵션 관리 API")
class ProductMenuOptionController(
    private val productMenuOptionService: ProductMenuOptionService,
) {
    @PostMapping
    @PreAuthorize("hasRole('MERCHANT')")
    fun saveMenuOptions(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @Valid @RequestBody groups: List<MenuOptionGroupRequest>,
    ): ResponseEntity<ApiResponse<List<ProductMenuOptionGroupResponse>>>

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('MERCHANT')")
    fun deleteMenuOptionGroup(
        @AuthenticationPrincipal userId: Long,
        @PathVariable productId: Long,
        @PathVariable groupId: Long,
    ): ResponseEntity<Void>
}
```

---

### 6.3 ProductController 변경

`createProduct()` — `ProductCreateRequest`에 `category`, `specs` 추가 (자동 반영, 변경 없음)
`getNearby()` — `ProductNearbyRequest`에 `category` 추가 (자동 반영, 변경 없음)

---

## 7. S3 설정

### 7.1 S3Config (신규)

**파일**: `app/src/main/kotlin/com/nearpick/app/config/S3Config.kt`

```kotlin
@Configuration
@ConditionalOnProperty("product.image.upload.enabled", havingValue = "true")
class S3Config(
    @Value("\${aws.s3.region:ap-northeast-2}") private val region: String,
    @Value("\${aws.access-key-id:}") private val accessKeyId: String,
    @Value("\${aws.secret-access-key:}") private val secretAccessKey: String,
) {
    @Bean
    fun s3Client(): S3Client = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        ))
        .build()

    @Bean
    fun s3Presigner(): S3Presigner = S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        ))
        .build()
}
```

### 7.2 build.gradle.kts 의존성 추가

```kotlin
// domain-nearpick/build.gradle.kts
implementation("software.amazon.awssdk:s3:2.25.23")
implementation("software.amazon.awssdk:s3-transfer-manager:2.25.23")
```

---

## 8. ErrorCode 추가

**파일**: `common/src/main/kotlin/com/nearpick/common/exception/ErrorCode.kt`

```kotlin
// Product Enhancement (Phase 11)
PRODUCT_IMAGE_LIMIT_EXCEEDED(400, "상품 이미지는 최대 5장까지 등록 가능합니다."),
PRODUCT_IMAGE_NOT_FOUND(404, "상품 이미지를 찾을 수 없습니다."),
INVALID_IMAGE_TYPE(400, "허용되지 않는 이미지 형식입니다. (허용: jpg, jpeg, png, webp)"),
MENU_OPTION_NOT_ALLOWED(400, "메뉴 옵션은 음식/음료 카테고리 상품에만 등록 가능합니다."),
MENU_OPTION_GROUP_NOT_FOUND(404, "메뉴 옵션 그룹을 찾을 수 없습니다."),
```

---

## 9. Flyway 마이그레이션

**파일**: `app/src/main/resources/db/migration/V5__product_enhancement.sql`

```sql
-- 1. products 테이블에 category, specs 컬럼 추가
ALTER TABLE products
    ADD COLUMN category VARCHAR(20) NULL AFTER product_type,
    ADD COLUMN specs TEXT NULL AFTER description;

CREATE INDEX idx_products_category ON products (category);

-- 2. product_images 테이블
CREATE TABLE product_images
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    product_id    BIGINT       NOT NULL,
    s3_key        VARCHAR(500) NOT NULL,
    url           VARCHAR(1000) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id) REFERENCES products (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);

-- 3. product_menu_option_groups 테이블
CREATE TABLE product_menu_option_groups
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    product_id    BIGINT      NOT NULL,
    name          VARCHAR(50) NOT NULL,
    required      BOOLEAN     NOT NULL DEFAULT FALSE,
    max_select    INT         NOT NULL DEFAULT 1,
    display_order INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_option_group_product
        FOREIGN KEY (product_id) REFERENCES products (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_menu_option_groups_product_id ON product_menu_option_groups (product_id);

-- 4. product_menu_choices 테이블
CREATE TABLE product_menu_choices
(
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    group_id         BIGINT      NOT NULL,
    name             VARCHAR(50) NOT NULL,
    additional_price INT         NOT NULL DEFAULT 0,
    display_order    INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_menu_choice_group
        FOREIGN KEY (group_id) REFERENCES product_menu_option_groups (id)
            ON DELETE CASCADE
);
```

---

## 10. 설정 추가

**`application.properties`** (기본값):
```properties
product.image.upload.enabled=false
aws.s3.bucket=
aws.s3.region=ap-northeast-2
aws.access-key-id=
aws.secret-access-key=
```

**`application-local.properties`** (local 전용):
```properties
# S3 사용 시 아래 설정 활성화 (기본은 mock mode)
# product.image.upload.enabled=true
# aws.s3.bucket=near-pick-dev-bucket
# aws.access-key-id=your_access_key
# aws.secret-access-key=your_secret_key
```

---

## 11. 캐시 무효화 전략

| 이벤트 | 무효화 캐시 |
|--------|-----------|
| 이미지 저장/삭제/순서 변경 | `products-detail::{productId}` |
| 메뉴 옵션 저장/삭제 | `products-detail::{productId}` |
| 상품 category/specs 업데이트 | `products-detail::{productId}`, `products-nearby` (category 기반 캐시 키) |

`products-nearby` 캐시 키에 `category` 추가:
```kotlin
key = "#request.category?.name + ':' + #request.locationSource.name + ':' + ..."
```

---

## 12. 테스트 전략

### 12.1 단위 테스트 (10개 이상)

| 테스트 클래스 | 케이스 |
|--------------|--------|
| `ProductImageServiceImplTest` | Presigned URL 발급 성공, 5장 초과 실패, 허용 안 되는 확장자 실패, URL 저장 성공, 이미지 삭제, 순서 변경 |
| `ProductMenuOptionServiceImplTest` | 메뉴 옵션 저장 성공, FOOD 이외 카테고리 실패, 그룹 삭제 성공 |
| `ProductServiceImplTest` (기존 확장) | category 필터 적용 확인, specs JSON 직렬화/역직렬화 |

### 12.2 테스트 파일 위치

```
domain-nearpick/src/test/kotlin/com/nearpick/nearpick/product/service/
  ├── ProductImageServiceImplTest.kt
  └── ProductMenuOptionServiceImplTest.kt
```

---

## 13. 파일 변경 목록

| 모듈 | 파일 | 변경 유형 |
|------|------|-----------|
| `common` | `exception/ErrorCode.kt` | 수정 (5개 추가) |
| `domain` | `product/ProductCategory.kt` | **신규** |
| `domain` | `product/dto/ProductImageDtos.kt` | **신규** |
| `domain` | `product/dto/ProductMenuOptionDtos.kt` | **신규** |
| `domain` | `product/ProductImageService.kt` | **신규** |
| `domain` | `product/ProductMenuOptionService.kt` | **신규** |
| `domain` | `product/dto/ProductDtos.kt` | 수정 (category, specs, images, menuOptions 추가) |
| `domain-nearpick` | `product/entity/ProductEntity.kt` | 수정 (category, specs 필드) |
| `domain-nearpick` | `product/entity/ProductImageEntity.kt` | **신규** |
| `domain-nearpick` | `product/entity/ProductMenuOptionGroupEntity.kt` | **신규** |
| `domain-nearpick` | `product/entity/ProductMenuChoiceEntity.kt` | **신규** |
| `domain-nearpick` | `product/repository/ProductImageRepository.kt` | **신규** |
| `domain-nearpick` | `product/repository/ProductMenuOptionGroupRepository.kt` | **신규** |
| `domain-nearpick` | `product/repository/ProductRepository.kt` | 수정 (category 필터 쿼리) |
| `domain-nearpick` | `product/client/S3Service.kt` | **신규** |
| `domain-nearpick` | `product/service/ProductImageServiceImpl.kt` | **신규** |
| `domain-nearpick` | `product/service/ProductMenuOptionServiceImpl.kt` | **신규** |
| `domain-nearpick` | `product/service/ProductServiceImpl.kt` | 수정 (category, specs, images, menuOptions 처리) |
| `domain-nearpick` | `product/mapper/ProductMapper.kt` | 수정 (category, images, menuOptions, specs 매핑) |
| `domain-nearpick` | `build.gradle.kts` | 수정 (AWS SDK 의존성) |
| `app` | `controller/ProductImageController.kt` | **신규** |
| `app` | `controller/ProductMenuOptionController.kt` | **신규** |
| `app` | `config/S3Config.kt` | **신규** |
| `app` | `resources/application.properties` | 수정 |
| `app` | `resources/db/migration/V5__product_enhancement.sql` | **신규** |

---

## 14. 구현 순서

```
Step 1. ErrorCode 추가 (common)
Step 2. ProductCategory enum + DTOs (domain)
         └── ProductImageDtos.kt, ProductMenuOptionDtos.kt
Step 3. 서비스 인터페이스 (domain)
         └── ProductImageService.kt, ProductMenuOptionService.kt
Step 4. ProductDtos.kt 수정 (domain)
         └── ProductDetailResponse, ProductSummaryResponse, ProductCreateRequest 수정
Step 5. AWS SDK 의존성 추가 (domain-nearpick/build.gradle.kts)
Step 6. V5__product_enhancement.sql (app/resources)
Step 7. ProductEntity 수정 + 신규 엔티티 3개 (domain-nearpick)
Step 8. Repository 3개 신규 + ProductRepository 수정 (domain-nearpick)
Step 9. S3Service (domain-nearpick)
Step 10. ProductImageServiceImpl + ProductMenuOptionServiceImpl (domain-nearpick)
Step 11. ProductServiceImpl 수정 (domain-nearpick)
Step 12. ProductMapper 수정 (domain-nearpick)
Step 13. S3Config (app)
Step 14. ProductImageController + ProductMenuOptionController (app)
Step 15. application.properties 수정 (app)
Step 16. 단위 테스트 작성 (domain-nearpick/test)
Step 17. ./gradlew build -x test → ./gradlew :app:bootRun 확인
```

---

## 15. 성공 기준

| 항목 | 기준 |
|------|------|
| 카테고리 등록·필터 | 상품 생성 시 category 저장, nearby?category=FOOD 필터 정상 |
| Presigned URL 발급 (mock) | `product.image.upload.enabled=false` 상태에서 mock URL 반환 |
| 이미지 URL 저장·조회 | `POST /products/{id}/images` → 저장 후 detail 응답에 포함 |
| 이미지 5장 제한 | 6번째 요청 시 `PRODUCT_IMAGE_LIMIT_EXCEEDED` 400 반환 |
| 메뉴 옵션 CRUD | FOOD 상품 옵션 그룹+선택지 저장·삭제, detail 응답에 포함 |
| FOOD 이외 옵션 거부 | BEAUTY 상품에 메뉴 옵션 등록 시 `MENU_OPTION_NOT_ALLOWED` 400 |
| specs 저장·조회 | BEAUTY 상품에 specs JSON 저장 후 detail에서 파싱 반환 |
| 빌드·테스트 통과 | `./gradlew build` 성공, 신규 테스트 10개 이상 통과 |
| Gap Analysis | Match Rate ≥ 90% |
