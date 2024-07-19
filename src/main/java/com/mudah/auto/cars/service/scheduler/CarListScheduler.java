package com.mudah.auto.cars.service.scheduler;

import com.mudah.auto.cars.persistence.AccessTokenRepository;
import com.mudah.auto.cars.service.CarListingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CarListScheduler {

    private final CarListingService carListingService;

    private final AccessTokenRepository accessTokenRepository;

    public CarListScheduler(CarListingService carListingService, AccessTokenRepository accessTokenRepository) {
        this.carListingService = carListingService;
        this.accessTokenRepository = accessTokenRepository;
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour on the hour
    public void fetchAndUpdateCarData() {


        try {
            //carListingService.getCarLists(accessToken);
        } catch (Exception e) {
            log.error("Error occurred while fetching and updating car data: ", e);
        }
    }
}
