package com.mealplanner.service;

import com.mealplanner.entity.*;
import com.mealplanner.repository.MenuItemRepository;
import com.mealplanner.repository.MealPatternRepository;
import com.mealplanner.repository.MealScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MealService {

    private final MenuItemRepository menuItemRepository;
    private final MealPatternRepository mealPatternRepository;
    private final MealScheduleRepository mealScheduleRepository;

    public MealService(MenuItemRepository menuItemRepository,
                       MealPatternRepository mealPatternRepository,
                       MealScheduleRepository mealScheduleRepository) {
        this.menuItemRepository = menuItemRepository;
        this.mealPatternRepository = mealPatternRepository;
        this.mealScheduleRepository = mealScheduleRepository;
    }

    // --- MenuItem CRUD ---
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public MenuItem saveMenuItem(MenuItem item) {
        return menuItemRepository.save(item);
    }

    public void deleteMenuItem(Long id) {
        menuItemRepository.deleteById(id);
    }

    public Optional<MenuItem> getMenuItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    // --- MealPattern CRUD ---
    public List<MealPattern> getAllMealPatterns() {
        return mealPatternRepository.findAllByOrderByPatternDayAscMealTypeAscDietTypeAsc();
    }

    public MealPattern saveMealPattern(MealPattern pattern) {
        // 저장할 때 가격 자동 계산
        pattern.setTotalPrice(calculateTotalPrice(pattern.getMenuNames()));
        return mealPatternRepository.save(pattern);
    }

    public void deleteMealPattern(Long id) {
        mealPatternRepository.deleteById(id);
    }

    public Optional<MealPattern> getMealPatternById(Long id) {
        return mealPatternRepository.findById(id);
    }

    // --- MealSchedule CRUD ---
    public List<MealSchedule> getScheduleByPeriod(LocalDate start, LocalDate end) {
        return mealScheduleRepository.findByMealDateBetween(start, end);
    }

    public List<MealSchedule> getScheduleByDate(LocalDate date) {
        return mealScheduleRepository.findByMealDate(date);
    }

    public MealSchedule saveMealSchedule(MealSchedule schedule) {
        // 저장할 때 가격 자동 계산
        schedule.setTotalPrice(calculateTotalPrice(schedule.getMenuNames()));
        return mealScheduleRepository.save(schedule);
    }

    public void deleteMealSchedule(Long id) {
        mealScheduleRepository.deleteById(id);
    }

    public Optional<MealSchedule> getMealScheduleById(Long id) {
        return mealScheduleRepository.findById(id);
    }

    // --- 비즈니스 로직 ---

    /**
     * 식단 문자열(쉼표, 슬래시, 줄바꿈 구분)을 분석하여 MenuItem 가격 합산
     */
    public int calculateTotalPrice(String menuNames) {
        if (menuNames == null || menuNames.trim().isEmpty()) {
            return 0;
        }
        String[] items = menuNames.split("[,\\n/]");
        int total = 0;
        for (String item : items) {
            String cleanItem = item.trim();
            if (cleanItem.isEmpty()) continue;
            total += menuItemRepository.findByName(cleanItem)
                    .map(MenuItem::getUnitPrice)
                    .orElse(0);
        }
        return total;
    }

    /**
     * 식단 검증 규칙
     * 1. 매운 음식(isSpicy=true)이 있으면 경고 발생
     * 2. 아침(MORNING) 또는 저녁(DINNER) 식단에 야채가 아닌 음식(isVegetable=false)이 있으면 경고 발생
     */
    public List<String> validateMeal(String menuNames, String mealTypeStr) {
        List<String> warnings = new ArrayList<>();
        if (menuNames == null || menuNames.trim().isEmpty()) {
            return warnings;
        }

        MealType mealType;
        try {
            mealType = MealType.valueOf(mealTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return warnings;
        }

        String[] items = menuNames.split("[,\\n/]");
        List<MenuItem> matchedItems = new ArrayList<>();
        for (String item : items) {
            String cleanItem = item.trim();
            if (cleanItem.isEmpty()) continue;
            menuItemRepository.findByName(cleanItem).ifPresent(matchedItems::add);
        }

        // 1. 매운맛 체크
        boolean hasSpicy = matchedItems.stream().anyMatch(MenuItem::isSpicy);
        if (hasSpicy) {
            List<String> spicyNames = matchedItems.stream()
                    .filter(MenuItem::isSpicy)
                    .map(MenuItem::getName)
                    .toList();
            warnings.add("매운 음식(" + String.join(", ", spicyNames) + ")이 포함되어 있어 어르신들께 자극적일 수 있습니다.");
        }

        // 2. 아침/저녁 채식 위주 식단 체크 (isVegetable = false인 항목 검출)
        if (mealType == MealType.MORNING || mealType == MealType.DINNER) {
            boolean hasNonVeg = matchedItems.stream().anyMatch(item -> !item.isVegetable());
            if (hasNonVeg) {
                List<String> nonVegNames = matchedItems.stream()
                        .filter(item -> !item.isVegetable())
                        .map(MenuItem::getName)
                        .toList();
                warnings.add(mealType.getDescription() + " 식단에 비채소(육류/생선 등) 메뉴(" +
                        String.join(", ", nonVegNames) + ")가 포함되어 있습니다. 아침과 저녁은 채식 위주의 부드러운 식단을 권장합니다.");
            }
        }

        return warnings;
    }

    /**
     * 21일간의 패턴을 특정 시작일자와 시작 패턴일을 기준으로 스케줄에 배포
     */
    public void deployPattern(LocalDate startDate, int startPatternDay) {
        for (int i = 0; i < 21; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            int targetPatternDay = (startPatternDay - 1 + i) % 21 + 1;

            // 해당 날짜의 기존 스케줄 삭제
            List<MealSchedule> existingSchedules = mealScheduleRepository.findByMealDate(currentDate);
            mealScheduleRepository.deleteAll(existingSchedules);

            // 해당 패턴 가져와서 스케줄로 복사
            List<MealPattern> patterns = mealPatternRepository.findByPatternDay(targetPatternDay);
            for (MealPattern pattern : patterns) {
                MealSchedule schedule = MealSchedule.builder()
                        .mealDate(currentDate)
                        .mealType(pattern.getMealType())
                        .dietType(pattern.getDietType())
                        .menuNames(pattern.getMenuNames())
                        .totalPrice(pattern.getTotalPrice())
                        .memo("패턴 " + targetPatternDay + "일차 자동 배포")
                        .build();
                mealScheduleRepository.save(schedule);
            }
        }
    }
}
