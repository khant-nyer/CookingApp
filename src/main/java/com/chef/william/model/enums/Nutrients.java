package com.chef.william.model.enums;

public enum Nutrients {
    // Energy
    CALORIES,

    // Macronutrients
    PROTEIN,
    CARBOHYDRATES,
    FAT,
    DIETARY_FIBER,      // ← preferred over plain "FIBER"
    SUGARS,             // ← note plural, matches frontend
    ADDED_SUGARS,
    CHOLESTEROL,

    // Fat Types
    SATURATED_FAT,
    MONOUNSATURATED_FAT,
    POLYUNSATURATED_FAT,
    TRANS_FAT,
    OMEGA_3,
    OMEGA_6,

    // Vitamins
    VITAMIN_A,
    VITAMIN_B1,         // Thiamin
    VITAMIN_B2,         // Riboflavin
    VITAMIN_B3,         // Niacin
    VITAMIN_B5,         // Pantothenic acid
    VITAMIN_B6,
    VITAMIN_B7,         // Biotin
    VITAMIN_B9,         // Folate / Folic acid
    VITAMIN_B12,
    VITAMIN_C,
    VITAMIN_D,
    VITAMIN_E,
    VITAMIN_K,
    CHOLINE,

    // Minerals
    CALCIUM,
    CHROMIUM,
    COPPER,
    IODINE,
    IRON,
    MAGNESIUM,
    MANGANESE,
    MOLYBDENUM,
    PHOSPHORUS,
    POTASSIUM,
    SELENIUM,
    SODIUM,
    ZINC
}