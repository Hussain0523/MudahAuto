package com.mudah.auto.cars.service;

import com.mudah.auto.cars.payload.CarListingResponse;
import com.mudah.auto.cars.service.dto.FilterGroup;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CarService {

    Mono<List<CarListingResponse>> getCarLists(String accessToken);

    Mono<CarListingResponse> getCarDetails(String id , String accessToken);

    Mono<Object> getCarListsFilter(String accessToken, String comparator, String apiName, Object value);

    Mono<Object> getCarListsMultiFilter(String accessToken, FilterGroup filterGroup) throws Exception;
}
