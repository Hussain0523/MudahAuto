package com.mudah.auto.cars.service;

import com.mudah.auto.cars.payload.CarCountResponse;
import com.mudah.auto.cars.payload.CarListingResponse;
import com.mudah.auto.cars.service.dto.FilterGroup;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface CarService {

    Mono<List<CarListingResponse>> getCarLists(String accessToken);

    Mono<CarListingResponse> getCarDetails(String id , String accessToken);

    Mono<Object> getCarListsMultiFilter(String accessToken, FilterGroup filterGroup) throws Exception;

    Mono<List<CarListingResponse>> getCarListsWithoutImages(String accessToken, Integer page, Integer perPage);

    Mono<List<String>> getCarImages(List<String> photoUrls);

    Mono<CarCountResponse> getTotalCount(String accessToken);
}
