package com.mudah.auto.cars.service;

import com.mudah.auto.cars.payload.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

public interface ZohoAuthService {

    Mono<TokenResponse> getAccessToken(String code);

    Mono<TokenResponse> getRefreshAccessToken();

    Mono<TokenResponse> getActiveToken();

}
