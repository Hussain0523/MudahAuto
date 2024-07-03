package com.mudah.auto.cars.controller;

import com.mudah.auto.cars.payload.TokenResponse;
import com.mudah.auto.cars.service.ZohoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/token")
@CrossOrigin
public class TokenController {


    private final ZohoAuthService zohoAuthService;

    public TokenController(ZohoAuthService zohoAuthService) {
        this.zohoAuthService = zohoAuthService;
    }

    @PostMapping("/getAccessToken")
    public Mono<ResponseEntity<TokenResponse>> getAccessToken(@RequestParam String code) {
        return zohoAuthService.getAccessToken(code)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @PostMapping("/getRefreshToken")
    public Mono<ResponseEntity<TokenResponse>> getRefreshAccessToken() {
        return zohoAuthService.getRefreshAccessToken()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @GetMapping("/getActiveToken")
    public Mono<ResponseEntity<TokenResponse>> getActiveToken() {
        return zohoAuthService.getActiveToken()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
