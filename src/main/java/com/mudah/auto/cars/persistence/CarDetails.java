package com.mudah.auto.cars.persistence;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
@Table("car_details")
public class CarDetails {

    @Id
    private UUID uuid;

    private String ownerName;
    private String ownerId;
    private String ownerEmail;
    private String extendedWarrantyProvider;
    private String currencySymbol;
    private String fullVariant;
    private String name;
    private LocalDateTime lastActivityTime;
    private Boolean extendedWarrantyProgramYesNo;
    private String carId;
    private String availability;
    private String variant;
    private String currentLocation;
    private BigDecimal totalRefurbishmentCost;
    private String purchaserHub;
    private BigDecimal tCecHandlingFee;
    private String photoUrl;
    private Integer mileageKm;
    private Integer yearOfEwp;
    private String carBrandCarMake;
    private String colour;
    private String carModel;
    private Integer yearMake;
    private BigDecimal listingPrice;
    private String approvalState;
    private BigDecimal totalPrice;
    private Boolean active;

    @CreatedDate
    private Instant createdDate;

    @CreatedBy
    private String createdBy;

    @LastModifiedDate
    private Instant modifiedDate;

    @LastModifiedBy
    private String modifiedBy;

}
