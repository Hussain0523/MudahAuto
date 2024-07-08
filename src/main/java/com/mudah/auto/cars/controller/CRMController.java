package com.mudah.auto.cars.controller;

import com.mudah.auto.cars.payload.CarListingResponse;
import com.mudah.auto.cars.service.CarService;
import com.mudah.auto.cars.service.dto.FilterGroup;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("api")
@CrossOrigin
public class CRMController {

    private final CarService carService;

    public CRMController(CarService carService) {
        this.carService = carService;
    }

    @PostMapping("/carLists")
    public Mono<ResponseEntity<List<CarListingResponse>>> getCarLists(@RequestParam String accessToken) {
        return carService.getCarLists(accessToken)
                .map(carLists -> ResponseEntity.ok(carLists))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping("/getCarDetails")
    public Mono<ResponseEntity<CarListingResponse>> getCarDetails(@RequestParam String id, @RequestParam String accessToken) {
        return carService.getCarDetails(id,accessToken)
                .map(carDetails -> ResponseEntity.ok(carDetails))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PostMapping("/carLists/filter")
    public Mono<ResponseEntity<Object>> getCarListsFilter(
            @RequestParam String accessToken,
            @RequestParam String comparator,
            @RequestParam String apiName,
            @RequestParam Object value) {

        return carService.getCarListsFilter(accessToken, comparator, apiName, value)
                .map(carLists -> ResponseEntity.ok(carLists)) // Wrap successful response
                .onErrorResume(e -> {
                    LoggerFactory.getLogger(this.getClass()).error("Error fetching car lists filter", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)); // Handle errors
                });
    }


    @PostMapping("/carLists/list/filter")
    public Mono<ResponseEntity<Object>> getCarListsMultiFilter(@RequestParam String accessToken, @RequestBody FilterGroup filterGroup) throws Exception{
        return carService.getCarListsMultiFilter(accessToken,filterGroup)
                .map(carLists -> ResponseEntity.ok(carLists))
                .onErrorResume(e -> {
                    LoggerFactory.getLogger(this.getClass()).error("Error fetching car lists filter", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
                });
    }

    @PostMapping("/carListsWithoutImages")
    public Mono<ResponseEntity<List<CarListingResponse>>> getCarListsWithoutImages(@RequestParam String accessToken) {
        return carService.getCarListsWithoutImages(accessToken)
                .map(carLists -> ResponseEntity.ok(carLists))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PostMapping("/carImages")
    public Mono<ResponseEntity<List<String>>> getCarImages(@RequestParam List<String> photoUrls) {
        return carService.getCarImages(photoUrls)
                .map(imageUrls -> ResponseEntity.ok(imageUrls))
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

}
