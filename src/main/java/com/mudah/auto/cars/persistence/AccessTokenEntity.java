package com.mudah.auto.cars.persistence;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
@Table("access_tokens")
public class AccessTokenEntity {

    @Id
    private UUID uuid;

    private String accessToken;
    private String refreshToken;
    @CreatedDate
    private Instant createdAt;
}
