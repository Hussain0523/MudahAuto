package com.mudah.auto.cars.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AccessTokenRepository extends ReactiveCrudRepository<AccessTokenEntity, UUID> {

    Mono<AccessTokenEntity> findByRefreshToken(String refreshToken);

}
