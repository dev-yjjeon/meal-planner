package com.mealplanner.config;

import com.mealplanner.entity.DietType;
import com.mealplanner.entity.MenuItem;
import com.mealplanner.entity.MealPattern;
import com.mealplanner.entity.MealType;
import com.mealplanner.repository.MenuItemRepository;
import com.mealplanner.repository.MealPatternRepository;
import com.mealplanner.service.MealService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private final MenuItemRepository menuItemRepository;
    private final MealPatternRepository mealPatternRepository;
    private final MealService mealService;

    public DataInitializer(MenuItemRepository menuItemRepository,
                           MealPatternRepository mealPatternRepository,
                           MealService mealService) {
        this.menuItemRepository = menuItemRepository;
        this.mealPatternRepository = mealPatternRepository;
        this.mealService = mealService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 1. Menu Items Seeding (20+ items)
        if (menuItemRepository.count() == 0) {
            List<MenuItem> items = List.of(
                    // 주식
                    new MenuItem(null, "쌀밥", "밥", 1000, false, true),
                    new MenuItem(null, "잡곡밥", "밥", 1200, false, true),
                    new MenuItem(null, "흰죽", "밥", 1000, false, true),
                    new MenuItem(null, "야채죽", "밥", 1500, false, true),
                    new MenuItem(null, "소고기죽", "밥", 2500, false, false),
                    // 국류
                    new MenuItem(null, "아욱된장국", "국", 1500, false, true),
                    new MenuItem(null, "시래기된장국", "국", 1500, false, true),
                    new MenuItem(null, "콩나물국", "국", 1200, false, true),
                    new MenuItem(null, "맑은미역국", "국", 1300, false, true),
                    new MenuItem(null, "소고기무국", "국", 2200, false, false),
                    new MenuItem(null, "계란파국", "국", 1200, false, false),
                    // 반찬류 (채식)
                    new MenuItem(null, "시금치나물", "반찬", 1000, false, true),
                    new MenuItem(null, "콩나물무침", "반찬", 1000, false, true),
                    new MenuItem(null, "고사리나물", "반찬", 1200, false, true),
                    new MenuItem(null, "무나물볶음", "반찬", 900, false, true),
                    new MenuItem(null, "애호박볶음", "반찬", 1100, false, true),
                    new MenuItem(null, "두부조림", "반찬", 1500, false, true),
                    new MenuItem(null, "연두부양념장", "반찬", 1300, false, true),
                    new MenuItem(null, "백김치", "반찬", 800, false, true),
                    new MenuItem(null, "배추김치", "반찬", 800, true, true),
                    new MenuItem(null, "오이무침", "반찬", 1000, true, true),
                    // 반찬류 (육류/생선/계란 - 비채소)
                    new MenuItem(null, "소불고기", "반찬", 3500, false, false),
                    new MenuItem(null, "돼지갈비찜", "반찬", 3500, false, false),
                    new MenuItem(null, "제육볶음", "반찬", 3000, true, false),
                    new MenuItem(null, "닭갈비", "반찬", 3200, true, false),
                    new MenuItem(null, "고등어구이", "반찬", 2800, false, false),
                    new MenuItem(null, "갈치구이", "반찬", 3300, false, false),
                    new MenuItem(null, "계란찜", "반찬", 1200, false, false),
                    new MenuItem(null, "멸치볶음", "반찬", 1500, false, false),
                    // 간식류
                    new MenuItem(null, "우유", "간식", 1000, false, false), // 동물성
                    new MenuItem(null, "두유", "간식", 1100, false, true),
                    new MenuItem(null, "바나나", "간식", 1200, false, true),
                    new MenuItem(null, "사과", "간식", 1000, false, true),
                    new MenuItem(null, "요거트", "간식", 1200, false, false)
            );
            menuItemRepository.saveAll(items);
        }

        // 2. Meal Patterns Seeding (21 Days for NORMAL diet type)
        if (mealPatternRepository.count() == 0) {
            List<MealPattern> patterns = new ArrayList<>();

            // 21일 루프 생성
            for (int day = 1; day <= 21; day++) {
                // 아침 (Morning) - 채식 위주
                String morningMenu = "쌀밥, 아욱된장국, 시금치나물, 백김치";
                patterns.add(MealPattern.builder()
                        .patternDay(day)
                        .mealType(MealType.MORNING)
                        .dietType(DietType.NORMAL)
                        .menuNames(morningMenu)
                        .totalPrice(mealService.calculateTotalPrice(morningMenu))
                        .build());

                // 오전 간식 (AM Snack)
                String amSnack = "바나나";
                patterns.add(MealPattern.builder()
                        .patternDay(day)
                        .mealType(MealType.AM_SNACK)
                        .dietType(DietType.NORMAL)
                        .menuNames(amSnack)
                        .totalPrice(mealService.calculateTotalPrice(amSnack))
                        .build());

                // 점심 (Lunch) - 온화한 육류/생선 단백질 포함
                String lunchMenu;
                if (day % 3 == 1) {
                    lunchMenu = "잡곡밥, 소고기무국, 소불고기, 백김치";
                } else if (day % 3 == 2) {
                    lunchMenu = "쌀밥, 맑은미역국, 고등어구이, 배추김치"; // 배추김치는 매운맛 테스트용
                } else {
                    lunchMenu = "잡곡밥, 계란파국, 갈치구이, 무나물볶음";
                }
                patterns.add(MealPattern.builder()
                        .patternDay(day)
                        .mealType(MealType.LUNCH)
                        .dietType(DietType.NORMAL)
                        .menuNames(lunchMenu)
                        .totalPrice(mealService.calculateTotalPrice(lunchMenu))
                        .build());

                // 오후 간식 (PM Snack)
                String pmSnack = (day % 2 == 0) ? "우유" : "두유";
                patterns.add(MealPattern.builder()
                        .patternDay(day)
                        .mealType(MealType.PM_SNACK)
                        .dietType(DietType.NORMAL)
                        .menuNames(pmSnack)
                        .totalPrice(mealService.calculateTotalPrice(pmSnack))
                        .build());

                // 저녁 (Dinner) - 소화 잘 되는 채식/두부 위주
                String dinnerMenu;
                if (day % 2 == 0) {
                    dinnerMenu = "쌀밥, 콩나물국, 두부조림, 백김치";
                } else {
                    dinnerMenu = "쌀밥, 시래기된장국, 연두부양념장, 애호박볶음";
                }
                patterns.add(MealPattern.builder()
                        .patternDay(day)
                        .mealType(MealType.DINNER)
                        .dietType(DietType.NORMAL)
                        .menuNames(dinnerMenu)
                        .totalPrice(mealService.calculateTotalPrice(dinnerMenu))
                        .build());
            }

            mealPatternRepository.saveAll(patterns);
        }
    }
}
