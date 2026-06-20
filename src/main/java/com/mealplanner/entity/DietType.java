package com.mealplanner.entity;

public enum DietType {
    NORMAL("일반식"),
    DIABETES("당뇨식"),
    CHOPPED("다진식"),
    GROUND("갈식"),
    TOFU("연두부식"),
    LIQUID_RICE("미음"),
    PORRIDGE("죽식");

    private final String description;

    DietType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
