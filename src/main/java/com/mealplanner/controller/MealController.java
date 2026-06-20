package com.mealplanner.controller;

import com.mealplanner.entity.DietType;
import com.mealplanner.entity.MealType;
import com.mealplanner.service.MealService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Controller
public class MealController {

    private final MealService mealService;

    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    @GetMapping("/")
    public String calendar(Model model) {
        model.addAttribute("mealTypes", MealType.values());
        model.addAttribute("dietTypes", DietType.values());
        return "calendar";
    }

    @GetMapping("/pattern")
    public String pattern(Model model) {
        model.addAttribute("patterns", mealService.getAllMealPatterns());
        model.addAttribute("mealTypes", MealType.values());
        model.addAttribute("dietTypes", DietType.values());
        return "pattern";
    }

    @GetMapping("/menu")
    public String menu(Model model) {
        model.addAttribute("menus", mealService.getAllMenuItems());
        return "menu";
    }

    @GetMapping("/print")
    public String print(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        if (startDate == null) {
            // 이번 주 월요일 설정
            startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if (endDate == null) {
            // 이번 주 일요일 설정
            endDate = startDate.plusDays(6);
        }

        List<LocalDate> dates = startDate.datesUntil(endDate.plusDays(1)).toList();

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("dates", dates);
        model.addAttribute("schedules", mealService.getScheduleByPeriod(startDate, endDate));
        model.addAttribute("mealTypes", MealType.values());
        model.addAttribute("dietTypes", DietType.values());
        return "print";
    }
}

