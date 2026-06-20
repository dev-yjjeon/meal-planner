package com.mealplanner.controller;

import com.mealplanner.entity.MealPattern;
import com.mealplanner.entity.MealSchedule;
import com.mealplanner.entity.MenuItem;
import com.mealplanner.service.MealService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MealApiController {

    private final MealService mealService;

    public MealApiController(MealService mealService) {
        this.mealService = mealService;
    }

    // --- MenuItem APIs ---
    @GetMapping("/menus")
    public List<MenuItem> getMenus() {
        return mealService.getAllMenuItems();
    }

    @PostMapping("/menus")
    public MenuItem saveMenu(@RequestBody MenuItem item) {
        return mealService.saveMenuItem(item);
    }

    @DeleteMapping("/menus/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable("id") Long id) {
        mealService.deleteMenuItem(id);
        return ResponseEntity.ok().build();
    }

    // --- MealPattern APIs ---
    @GetMapping("/patterns")
    public List<MealPattern> getPatterns() {
        return mealService.getAllMealPatterns();
    }

    @PostMapping("/patterns")
    public MealPattern savePattern(@RequestBody MealPattern pattern) {
        return mealService.saveMealPattern(pattern);
    }

    @DeleteMapping("/patterns/{id}")
    public ResponseEntity<Void> deletePattern(@PathVariable("id") Long id) {
        mealService.deleteMealPattern(id);
        return ResponseEntity.ok().build();
    }

    // --- MealSchedule APIs ---
    @GetMapping("/schedules")
    public List<MealSchedule> getSchedules(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return mealService.getScheduleByPeriod(startDate, endDate);
    }

    @PostMapping("/schedules")
    public ResponseEntity<Map<String, Object>> saveSchedule(@RequestBody MealSchedule schedule) {
        // 실시간 검증 실행
        List<String> warnings = mealService.validateMeal(schedule.getMenuNames(), schedule.getMealType().name());

        // 가격 자동 계산 및 일정 저장
        MealSchedule saved = mealService.saveMealSchedule(schedule);

        Map<String, Object> response = new HashMap<>();
        response.put("schedule", saved);
        response.put("warnings", warnings);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable("id") Long id) {
        mealService.deleteMealSchedule(id);
        return ResponseEntity.ok().build();
    }

    // --- 패턴 일괄 배포 ---
    @PostMapping("/schedules/deploy")
    public ResponseEntity<Map<String, String>> deploy(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("startPatternDay") int startPatternDay) {
        mealService.deployPattern(startDate, startPatternDay);
        Map<String, String> response = new HashMap<>();
        response.put("message", "21일간의 패턴 배포가 성공적으로 완료되었습니다.");
        return ResponseEntity.ok(response);
    }

    // --- 실시간 식단 검증 및 가격 계산 API ---
    @GetMapping("/schedules/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestParam("menuNames") String menuNames,
            @RequestParam("mealType") String mealType) {
        List<String> warnings = mealService.validateMeal(menuNames, mealType);
        int totalPrice = mealService.calculateTotalPrice(menuNames);

        Map<String, Object> response = new HashMap<>();
        response.put("warnings", warnings);
        response.put("totalPrice", totalPrice);
        return ResponseEntity.ok(response);
    }
}
