package com.mealplanner.repository;

import com.mealplanner.entity.DietType;
import com.mealplanner.entity.MealPattern;
import com.mealplanner.entity.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MealPatternRepository extends JpaRepository<MealPattern, Long> {
    List<MealPattern> findByPatternDay(int patternDay);
    List<MealPattern> findByDietType(DietType dietType);
    Optional<MealPattern> findByPatternDayAndMealTypeAndDietType(int patternDay, MealType mealType, DietType dietType);
    List<MealPattern> findAllByOrderByPatternDayAscMealTypeAscDietTypeAsc();
}
