# 요양원 식단 플래너 (Meal Planner) 구현 가이드 및 명세서

본 프로젝트는 요양원의 영양 기준(아침/저녁 채식 위주, 마일드한 단백질 중심 점심)과 개별 어르신들의 질감/치료식 요구사항을 효율적으로 설계하고, 실시간으로 식단가 계산 및 건강 경고를 관리하기 위해 작성된 **Spring Boot 3.3.0 (Java 21, Maven)** 기반 프로젝트입니다.

---

## 1. 파일 구조 (Created Files)

```
meal-planner/
├── pom.xml                                   # 프로젝트 빌드 구성 및 의존성
├── walkthrough.md                            # 프로젝트 구현 명세 (본 문서)
└── src/
    └── main/
        ├── java/com/mealplanner/
        │   ├── MealPlannerApplication.java   # Spring Boot 메인 클래스
        │   ├── config/
        │   │   └── DataInitializer.java      # 구동 시 기본 데이터 세팅 (20개+ 메뉴, 21일 패턴)
        │   ├── entity/
        │   │   ├── MenuItem.java             # 식자재 및 메뉴 단가 DB Entity
        │   │   ├── MealPattern.java          # 21일 식단 템플릿 패턴 Entity
        │   │   ├── MealSchedule.java         # 실제 달력에 배정된 일정 Entity
        │   │   ├── MealType.java             # 식사 유형 Enum (MORNING, LUNCH, DINNER 등)
        │   │   └── DietType.java             # 치료식/질감 구분 Enum (NORMAL, DIABETES 등)
        │   ├── repository/
        │   │   ├── MenuItemRepository.java   # 메뉴 아이템 JPA Repository
        │   │   ├── MealPatternRepository.java# 식단 패턴 JPA Repository
        │   │   └── MealScheduleRepository.java# 식단 일정 JPA Repository
        │   ├── service/
        │   │   └── MealService.java          # 핵심 비즈니스 로직 (단가 합산, 유효성 검사, 패턴 배포)
        │   └── controller/
        │       ├── MealController.java       # Thymeleaf HTML 페이지 뷰 컨트롤러
        │       └── MealApiController.java    # AJAX/REST CRUD 및 실시간 검증 API 컨트롤러
        └── resources/
            ├── application.properties        # 포트(8082) 및 H2 File Database 설정
            ├── static/
            │   ├── css/
            │   │   └── style.css             # 모던 파스텔 다크/라이트 테마 및 인쇄용 A4 CSS
            │   └── js/
            │       └── app.js                # 달력 렌더링, 모달 팝업, 실시간 가격/경고, AJAX 통신
            └── templates/
                ├── calendar.html             # 메인 캘린더 화면
                ├── pattern.html              # 21일 패턴 목록/편집 화면
                ├── menu.html                 # 식자재 단가 및 영양 기준 데이터 화면
                └── print.html                # A4 가로 인쇄용 식단표 화면
```

---

## 2. 논리적 변경 사항 및 기능 명세

### 1) 실시간 식단가 계산 (`MealService.calculateTotalPrice`)
- 사용자가 식단 입력란에 메뉴명을 입력할 때, 쉼표(`,`), 슬래시(`/`), 또는 줄바꿈(`\n`) 기준으로 단어를 쪼갭니다.
- 데이터베이스(`MenuItem`)에 일치하는 이름이 등록되어 있다면 해당 단가를 조회하여 실시간으로 합산합니다.
- 예: `"쌀밥, 아욱된장국, 소불고기, 백김치"` 입력 시
  - `쌀밥(1000원) + 아욱된장국(1500원) + 소불고기(3500원) + 백김치(800원)` = **6,800원** 자동 계산.

### 2) 영양 및 소화 유효성 검사 (`MealService.validateMeal`)
- **매운맛 경고 (Spicy Warning)**: 입력된 메뉴 중 `isSpicy=true`인 메뉴가 하나라도 있을 경우 경고 메시지를 노출합니다.
  - 예: `"제육볶음"`이나 `"닭갈비"` 포함 시: `"매운 음식(제육볶음)이 포함되어 있어 어르신들께 자극적일 수 있습니다."`
- **아침/저녁 비채소(육류/생선) 경고 (Morning/Dinner Veg Warning)**: 소화 기능이 약한 어르신들을 위해 아침(`MORNING`) 또는 저녁(`DINNER`) 식단에 `isVegetable=false`인 동물성 메뉴(육류, 생선 등)가 포함되어 있을 경우 경고를 표시합니다.
  - 예: 아침에 `"소불고기"` 포함 시: `"아침 식단에 비채소(육류/생선 등) 메뉴(소불고기)가 포함되어 있습니다. 아침과 저녁은 채식 위주의 부드러운 식단을 권장합니다."`

### 3) 21일 식단 패턴 배포 알고리즘 (`MealService.deployPattern`)
- 사용자가 지정한 시작일자(`startDate`)로부터 21일간의 스케줄을 자동으로 생성합니다.
- `startPatternDay`(1~21)에 매핑되는 패턴부터 시작하여 날짜가 지나감에 따라 패턴 일차가 순환(`1 -> 2 -> ... -> 21 -> 1`)하며 MealSchedule에 복사 저장됩니다.
- 배포를 실행할 때, 이미 해당 기간에 생성되어 있던 스케줄은 자동으로 덮어씌워져(삭제 후 신규 삽입) 데이터 중복을 방지합니다.

---

## 3. DB 스키마 설계 및 데이터 시딩

### 1) DB 연결 설정
- H2 로컬 파일 데이터베이스를 사용하여 서버 종료 시에도 데이터가 보존됩니다.
- 경로: `./data/mealplanner.mv.db`
- 콘솔 접속 주소: `http://localhost:8082/h2-console` (JDBC URL: `jdbc:h2:file:./data/mealplanner`)

### 2) 시드 데이터
- **메뉴 데이터 (20개 이상)**: 쌀밥, 잡곡밥, 흰죽, 야채죽, 아욱된장국, 소불고기(비채식/순한맛), 제육볶음(비채식/매운맛), 닭갈비(비채식/매운맛), 시금치나물(채식/순한맛), 배추김치(채식/매운맛) 등 30여 개 품목 세팅.
- **패턴 데이터 (21일분)**: 요양원 기준에 맞춰 아침/저녁은 시금치나물, 두부조림, 연두부, 콩나물국 등 순한 채식 위주로 세팅되었으며, 점심은 소불고기, 고등어구이 등 온화한 단백질 중심으로 구성된 21일간의 기준 템플릿 제공.

---

## 4. 영향도 평가 (Side Effect) 및 수동 테스트 포인트
- **DB Lock 가능성**: Windows OS 특성상 H2 파일 DB가 동시 사용될 경우 파일 락이 발생할 수 있습니다. IntelliJ 내부 실행과 콘솔 접근이 원활하도록 `AUTO_SERVER=TRUE` 옵션을 연결 문자열에 추가하여 보완하였습니다.
- **테스트 시 확인 포인트**:
  1. **캘린더 화면(`http://localhost:8082/`)**에서 날짜를 누르고 식단 관리 모달을 실행한 뒤, 텍스트에 `"닭갈비"`를 입력하여 **매운맛 경고** 및 실시간 단가 합산이 작동하는지 검증합니다.
  2. 아침식사 식단에 `"소불고기"`를 입력하여 **비채소 경고**가 화면 상단 경고 박스에 표시되는지 검증합니다.
  3. **인쇄 화면(`/print`)**으로 이동하여 원하는 출력 기간을 설정하고 "인쇄하기" 버튼을 눌러 인쇄 미리보기 화면에서 헤더/푸터 및 네비게이션바가 사라진 가로 A4 양식으로 표가 깔끔히 렌더링되는지 확인합니다.

---

## 5. 실행 방법 (How to Run)
1. 프로젝트 경로(`c:\Users\dudwo\OneDrive\바탕 화면\업체\전영재\meal-planner`)에서 터미널을 실행하거나 IntelliJ로 엽니다.
2. (사용자 조작 위임) Maven 빌드 플러그인 또는 IDE의 Run 실행 단추를 누릅니다.
   ```powershell
   # Maven 직접 구동 시 (사용자 실행 권장)
   ./mvnw spring-boot:run
   ```
3. 브라우저로 `http://localhost:8082/`에 접속합니다.
