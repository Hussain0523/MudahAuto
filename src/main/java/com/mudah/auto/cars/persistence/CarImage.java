package com.mudah.auto.cars.persistence;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Getter
@Setter
@Table("car_images")
public class CarImage {

    @Id
    private UUID uuid;

    private String carId;
    private String carName;
    private String imageUrl;

    @CreatedDate
    private Instant createdDate;

    @CreatedBy
    private String createdBy;

    @LastModifiedDate
    private Instant modifiedDate;

    @LastModifiedBy
    private String modifiedBy;
}
