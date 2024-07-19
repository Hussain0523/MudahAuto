package com.mudah.auto.cars.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface CarImageRepository extends ReactiveCrudRepository<CarImage, UUID> {
}
