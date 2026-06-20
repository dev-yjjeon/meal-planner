package com.mealplanner.repository;

import com.mealplanner.entity.DietType;
import com.mealplanner.entity.MealSchedule;
import com.mealplanner.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealScheduleRepository extends JpaRepository<MealSchedule, Long> {
    List<MealSchedule> findByMealDateBetween(LocalDate startDate, LocalDate endDate);
    List<MealSchedule> findByMealDate(LocalDate mealDate);
    Optional<MealSchedule> findByMealDateAndMealTypeAndDietType(LocalDate mealDate, MealType mealType, DietType dietType);
}
