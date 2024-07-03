package com.mudah.auto.cars.service;

import com.mudah.auto.cars.service.dto.FilterGroup;
import reactor.core.publisher.Mono;

public interface CarService {

    Mono<Object> getCarLists(String accessToken);

    Mono<Object> getCarDetails(String id , String accessToken);

    Mono<Object> getCarListsFilter(String accessToken, String comparator, String apiName, Object value);

    Mono<Object> getCarListsMultiFilter(String accessToken, FilterGroup filterGroup) throws Exception;
}
