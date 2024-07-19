package com.mudah.auto.cars.service;

import com.mudah.auto.cars.payload.CarListingResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CarListingService {

    Mono<List<CarListingResponse>> getCarLists(String accessToken);
}
