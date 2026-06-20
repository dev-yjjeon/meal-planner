package com.mealplanner.entity;

public enum MealType {
    MORNING("아침"),
    AM_SNACK("오전간식"),
    LUNCH("점심"),
    PM_SNACK("오후간식"),
    DINNER("저녁");

    private final String description;

    MealType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
