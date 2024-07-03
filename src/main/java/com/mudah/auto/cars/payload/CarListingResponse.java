package com.mudah.auto.cars.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class CarListingResponse {

    private Map<String, Object> carDetails;
    private List<String> imageURLs;

}
