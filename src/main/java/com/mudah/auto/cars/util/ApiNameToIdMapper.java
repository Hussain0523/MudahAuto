package com.mudah.auto.cars.util;

import org.springframework.stereotype.Component;

@Component
public class ApiNameToIdMapper {

    public static String getIdForApiName(String apiName) {
        switch (apiName) {
            case "Name":
                return "5741151000000625589";
            case "Availability":
                return "5741151000000644381";
            case "Listing_Price":
                return "5741151000000654013";
            case "Car_Brand_Car_Make":
                return "5741151000000643542";
            case "Car_Model":
                return "5741151000000640008";
            case "Variant":
                return "5741151000000641085";
            case "Year_Make":
                return "5741151000000640947";
            case "Full_Variant":
                return "5741151000000639996";
            case "Purchaser_Hub":
                return "5741151000010573053";
            case "Mileage_km":
                return "5741151000000637617";
            case "Owner":
                return "5741151000000625591";
            case "Colour":
                return "5741151000000637610";
            case "Total_Refurbishment_Cost":
                return "5741151000000655270";
            case "Current_Location":
                return "5741151000000647013";
            default:
                throw new IllegalArgumentException("Invalid api_name: " + apiName);
        }
    }
}