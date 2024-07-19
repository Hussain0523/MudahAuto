package com.mudah.auto.cars.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.drive.model.File;
import com.mudah.auto.cars.payload.CarListingResponse;
import com.mudah.auto.cars.persistence.CarDetails;
import com.mudah.auto.cars.persistence.CarDetailsRepository;
import com.mudah.auto.cars.persistence.CarImage;
import com.mudah.auto.cars.persistence.CarImageRepository;
import com.mudah.auto.cars.service.CarListingService;
import com.mudah.auto.cars.service.dto.CarDetailsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CarListingServiceImpl implements CarListingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient;

    private final GoogleServiceImpl googleService;

    private final CarDetailsRepository carDetailsRepository;

    private final CarImageRepository carImageRepository;

    public CarListingServiceImpl(WebClient webClient, GoogleServiceImpl googleService, CarDetailsRepository carDetailsRepository, CarImageRepository carImageRepository) {
        this.webClient = webClient;
        this.googleService = googleService;
        this.carDetailsRepository = carDetailsRepository;
        this.carImageRepository = carImageRepository;
    }


    @Override
    public Mono<List<CarListingResponse>> getCarLists(String accessToken) {
        String url = "https://crm.zoho.com/crm/v2.2/Inventories/bulk?";

        return Mono.fromCallable(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Zoho-oauthtoken " + accessToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
            }

            Map<String, Object> carDetailsResponse = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) carDetailsResponse.get("data");

            return processCarData(data);
        });
    }

    private List<CarListingResponse> processCarData(List<Map<String, Object>> data) {
        List<CarListingResponse> carListingResponses = new ArrayList<>();

        for (Map<String, Object> carData : data) {
            String photoUrl = (String) carData.get("Photo_URL");
            String id = (String) carData.get("id");
            List<String> imageUrls = new ArrayList<>();

            if (photoUrl != null && !photoUrl.isEmpty()) {
                try {
                    String folderId = extractFolderIdFromUrl(photoUrl);
                    List<File> imageFiles = googleService.retrieveImageFiles(folderId);

                    for (File file : imageFiles) {
                        String webViewLink = file.getWebViewLink();
                        imageUrls.add(webViewLink);

                        saveCarImage(id, (String) carData.get("Name"), webViewLink);
                    }
                } catch (Exception e) {
                    log.error("Error processing photo URL: " + photoUrl, e);
                }
            }

            CarDetailsDTO carDetailsDTO = objectMapper.convertValue(carData, CarDetailsDTO.class);
            CarDetails carDetails = convertToCarDetailsEntity(carDetailsDTO, photoUrl);

            carDetailsRepository.save(carDetails).subscribe();
            carListingResponses.add(new CarListingResponse(carData, imageUrls));
        }

        return carListingResponses;
    }

    private void saveCarImage(String carId, String carName, String imageUrl) {
        CarImage carImage = new CarImage();
        carImage.setCarId(carId);
        carImage.setCarName(carName);
        carImage.setImageUrl(imageUrl);
        carImage.setCreatedDate(Instant.now());
        carImage.setCreatedBy("system");
        carImage.setModifiedDate(Instant.now());
        carImage.setModifiedBy("system");
        carImageRepository.save(carImage).subscribe();
    }

    private CarDetails convertToCarDetailsEntity(CarDetailsDTO dto, String photoUrl) {
        CarDetails entity = new CarDetails();
        entity.setUuid(UUID.randomUUID());
        entity.setOwnerName(dto.getOwnerName());
        entity.setOwnerId(dto.getOwnerId());
        entity.setOwnerEmail(dto.getOwnerEmail());
        entity.setExtendedWarrantyProvider(dto.getExtendedWarrantyProvider());
        entity.setCurrencySymbol(dto.getCurrencySymbol());
        entity.setFullVariant(dto.getFullVariant());
        entity.setName(dto.getName());
        entity.setLastActivityTime(dto.getLastActivityTime());
        entity.setExtendedWarrantyProgramYesNo(dto.getExtendedWarrantyProgramYesNo());
        entity.setCarId(dto.getCarId());
        entity.setAvailability(dto.getAvailability());
        entity.setVariant(dto.getVariant());
        entity.setCurrentLocation(dto.getCurrentLocation());
        entity.setTotalRefurbishmentCost(dto.getTotalRefurbishmentCost());
        entity.setPurchaserHub(dto.getPurchaserHub());
        entity.setTCecHandlingFee(dto.getTCecHandlingFee());
        entity.setPhotoUrl(photoUrl);
        entity.setMileageKm(dto.getMileageKm());
        entity.setYearOfEwp(dto.getYearOfEwp());
        entity.setCarBrandCarMake(dto.getCarBrandCarMake());
        entity.setColour(dto.getColour());
        entity.setCarModel(dto.getCarModel());
        entity.setYearMake(dto.getYearMake());
        entity.setListingPrice(dto.getListingPrice());
        entity.setApprovalState(dto.getApprovalState());
        entity.setActive(dto.getActive());
        BigDecimal listingPrice = dto.getListingPrice() != null ? dto.getListingPrice() : BigDecimal.ZERO;
        BigDecimal handlingFee = dto.getTCecHandlingFee() != null ? dto.getTCecHandlingFee() : BigDecimal.ZERO;
        entity.setTotalPrice(listingPrice.add(handlingFee));
        entity.setCreatedDate(Instant.now());
        entity.setCreatedBy("system");
        entity.setModifiedDate(Instant.now());
        entity.setModifiedBy("system");

        return entity;
    }

    private String extractFolderIdFromUrl(String url) {
        Pattern pattern = Pattern.compile("https://drive\\.google\\.com/drive/folders/([a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid folder URL");
    }
}
