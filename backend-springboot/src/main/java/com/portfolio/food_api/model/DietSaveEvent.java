package com.portfolio.food_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DietSaveEvent {
    private String userId;
    private String mealTime;
    private String type;
}