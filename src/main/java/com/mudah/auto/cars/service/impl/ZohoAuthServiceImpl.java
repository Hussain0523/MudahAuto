package com.mudah.auto.cars.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mudah.auto.cars.payload.TokenResponse;
import com.mudah.auto.cars.persistence.AccessTokenEntity;
import com.mudah.auto.cars.persistence.AccessTokenRepository;
import com.mudah.auto.cars.service.ZohoAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
@Slf4j
public class ZohoAuthServiceImpl implements ZohoAuthService {

    private final WebClient webClient;

    private final AccessTokenRepository accessTokenRepository;

    @Value("${zoho.oauth.url}")
    private String zohoOAuthUrl;

    @Value("${zoho.oauth.client-id}")
    private String clientId;

    @Value("${zoho.oauth.client-secret}")
    private String clientSecret;

    @Value("${zoho.oauth.refreshToken}")
    private String refreshToken;

    @Value("${zoho.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${zoho.oauth.grant-type.access}")
    private String grantTypeAccess;

    @Value("${zoho.oauth.grant-type.refresh}")
    private String grantTypeRefresh;

//    @Value("${zoho.oauth.code}")
//    private String code;

    public ZohoAuthServiceImpl(WebClient.Builder webClientBuilder, AccessTokenRepository accessTokenRepository) {
        this.webClient = webClientBuilder
                .baseUrl("https://accounts.zoho.com")
                .build();
        this.accessTokenRepository = accessTokenRepository;
    }

    @Override
    public Mono<TokenResponse> getAccessToken(String code) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(zohoOAuthUrl)
                        .queryParam("grant_type", grantTypeAccess)
                        .queryParam("code", code)
                        .queryParam("client_id", clientId)
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("client_secret", clientSecret)
                        .build()
                )
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(String.class) // Get raw response as String
                .doOnNext(response -> log.info("Raw response: {}", response))
                .map(response -> {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        return mapper.readValue(response, TokenResponse.class);
                    } catch (JsonProcessingException e) {
                        log.error("Error parsing response: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                .doOnError(e -> log.error("Error fetching access token from Zoho: {}", e.getMessage()));
    }


//    @Override
//    public Mono<TokenResponse> getRefreshAccessToken() {
//        return webClient.post()
//                .uri(uriBuilder -> uriBuilder
//                        .path(zohoOAuthUrl)
//                        .queryParam("refresh_token", refreshToken)
//                        .queryParam("grant_type", grantTypeRefresh)
//                        .queryParam("client_id", clientId)
//                        .queryParam("client_secret", clientSecret)
//                        .build()
//                )
//                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                .retrieve()
//                .bodyToMono(TokenResponse.class)
//                .doOnError(e -> log.error("Error refreshing access token from Zoho: {}", e.getMessage()));
//    }

    @Override
    public Mono<TokenResponse> getRefreshAccessToken() {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(zohoOAuthUrl)
                        .queryParam("refresh_token", refreshToken)
                        .queryParam("grant_type", grantTypeRefresh)
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .build()
                )
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .flatMap(tokenResponse -> accessTokenRepository.findByRefreshToken(refreshToken)
                        .flatMap(existingTokenEntity -> {
                            existingTokenEntity.setAccessToken(tokenResponse.getAccessToken());
                            existingTokenEntity.setCreatedAt(Instant.now());
                            return accessTokenRepository.save(existingTokenEntity);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            AccessTokenEntity newTokenEntity = new AccessTokenEntity();
                            newTokenEntity.setAccessToken(tokenResponse.getAccessToken());
                            newTokenEntity.setRefreshToken(refreshToken);
                            return accessTokenRepository.save(newTokenEntity);
                        }))
                        .thenReturn(tokenResponse))
                .doOnError(e -> log.error("Error refreshing access token from Zoho: {}", e.getMessage()));
    }

    @Override
    public Mono<TokenResponse> getActiveToken() {
        return accessTokenRepository.findByRefreshToken(refreshToken)
                .flatMap(tokenEntity -> {
                    if (Duration.between(tokenEntity.getCreatedAt(), Instant.now()).toMinutes() < 50) {
                        TokenResponse tokenResponse = new TokenResponse();
                        tokenResponse.setAccessToken(tokenEntity.getAccessToken());
                        return Mono.just(tokenResponse);
                    } else {
                        return accessTokenRepository.save(tokenEntity)
                                .then(getRefreshAccessToken());
                    }
                });
    }

}
