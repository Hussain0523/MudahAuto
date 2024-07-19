package com.mudah.auto.cars.service.dto;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
@Getter
@Setter
public class CarDetailsDTO {

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
}
