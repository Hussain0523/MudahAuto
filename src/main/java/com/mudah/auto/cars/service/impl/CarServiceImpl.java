package com.mudah.auto.cars.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mudah.auto.cars.payload.CarListingResponse;
import com.mudah.auto.cars.service.CarService;
import com.mudah.auto.cars.service.dto.Field;
import com.mudah.auto.cars.service.dto.Filter;
import com.mudah.auto.cars.service.dto.FilterGroup;
import com.mudah.auto.cars.util.ApiNameToIdMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CarServiceImpl implements CarService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient;

    private final GoogleServiceImpl googleService;

    public CarServiceImpl(WebClient webClient, GoogleServiceImpl googleService) {
        this.webClient = webClient;
        this.googleService = googleService;
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
            if (response.statusCode() == 200) {
                Map<String, Object> carDetails = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) carDetails.get("data");

                List<CarListingResponse> carListingResponses = new ArrayList<>();
                for (Map<String, Object> carData : data) {
                    String photoUrl = (String) carData.get("Photo_URL");
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        String folderId = extractFolderIdFromUrl(photoUrl);

                        List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);
                        List<String> imageUrls = new ArrayList<>();
                        for (com.google.api.services.drive.model.File file : imageFiles) {
                            imageUrls.add(file.getWebViewLink());
                        }

                        carListingResponses.add(new CarListingResponse(carData, imageUrls));
                    } else {
                        carListingResponses.add(new CarListingResponse(carData, new ArrayList<>()));
                    }
                }

                return carListingResponses;
            } else {
                throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
            }
        });
    }

    @Override
    public Mono<CarListingResponse> getCarDetails(String id, String accessToken) {
        String url = "https://crm.zoho.com/crm/v2.2/Inventories/" + id +
                "?approved=both&converted=both&formatted_currency=true&home_converted_currency=true&on_demand_properties=%24client_portal_permission";

        return Mono.fromCallable(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Zoho-oauthtoken " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> carDetails = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) carDetails.get("data");
                if (!data.isEmpty()) {
                    Map<String, Object> carData = data.get(0);
                    String photoUrl = (String) carData.get("Photo_URL");
                    String folderId = extractFolderIdFromUrl(photoUrl);

                    List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);
                    List<String> imageUrls = new ArrayList<>();
                    for (com.google.api.services.drive.model.File file : imageFiles) {
                        imageUrls.add(file.getWebViewLink());
                    }

                    log.info("Successfully fetched car details and image URLs for id: {}", id);
                    return new CarListingResponse(carData, imageUrls);
                } else {
                    log.warn("No car data found for id: {}", id);
                    throw new RuntimeException("No car data found.");
                }
            } else {
                log.error("Failed to fetch data for id: {}. Status code: {}", id, response.statusCode());
                throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
            }
        });
    }

    private String extractFolderIdFromUrl(String url) {
        Pattern pattern = Pattern.compile("https://drive\\.google\\.com/drive/folders/([a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid folder URL");
    }

    @Override
    public Mono<Object> getCarListsFilter(String accessToken, String comparator, String apiName, Object value) {
        String url = "https://crm.zoho.com/crm/v2.1/Inventories/bulk";

        String fieldId;
        try {
            fieldId = ApiNameToIdMapper.getIdForApiName(apiName);
        } catch (IllegalArgumentException e) {
            return Mono.error(e);
        }
        Filter filter = new Filter(comparator, new Field(apiName, fieldId), value);
        String formData = "cvid=" + URLEncoder.encode("5741151000000625050", StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        String filters;
        try {
            filters = URLEncoder.encode(objectMapper.writeValueAsString(filter), StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to encode filter JSON", e));
        }
        formData += "&filters=" + filters;

        final String finalUrl = url;
        final String finalAccessToken = accessToken;
        final String finalFormData = formData;

        return Mono.fromCallable(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(finalUrl))
                        .header("Authorization", "Zoho-oauthtoken " + finalAccessToken)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(finalFormData))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Map<String, Object> jsonResponse = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                    List<Map<String, Object>> data = (List<Map<String, Object>>) jsonResponse.get("data");

                    List<CarListingResponse> carListings = new ArrayList<>();
                    for (Map<String, Object> carData : data) {
                        String photoUrl = (String) carData.get("Photo_URL");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            String folderId = extractFolderIdFromUrl(photoUrl);
                            List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);
                            List<String> imageUrls = new ArrayList<>();
                            for (com.google.api.services.drive.model.File file : imageFiles) {
                                imageUrls.add(file.getWebViewLink());
                            }
                            carListings.add(new CarListingResponse(carData, imageUrls));
                        } else {
                            log.warn("No photo URL found in car data");
                        }
                    }

                    if (carListings.isEmpty()) {
                        log.warn("No car data found in response");
                        throw new RuntimeException("No car data found in response");
                    }

                    log.info("Successfully fetched {} car listings", carListings.size());
                    return carListings;
                } else {
                    log.error("Failed to fetch data. Status code: {}", response.statusCode());
                    throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                LoggerFactory.getLogger(this.getClass()).error("HTTP request failed", e);
                throw new RuntimeException("Failed to send HTTP request", e);
            }
        });
    }


    @Override
    public Mono<Object> getCarListsMultiFilter(String accessToken, FilterGroup filterGroup) throws Exception {
        String url = "https://crm.zoho.com/crm/v2.1/Inventories/bulk";
        String cvidParam = "cvid=" + URLEncoder.encode("5741151000000625050", StandardCharsets.UTF_8);
        String filtersParam = prepareFiltersParameter(filterGroup);
        String formData = cvidParam + "&filters=" + filtersParam;

        return Mono.fromCallable(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Zoho-oauthtoken " + accessToken)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formData))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Map<String, Object> jsonResponse = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                    List<Map<String, Object>> data = (List<Map<String, Object>>) jsonResponse.get("data");

                    List<CarListingResponse> carListings = new ArrayList<>();
                    for (Map<String, Object> carData : data) {
                        String photoUrl = (String) carData.get("Photo_URL");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            String folderId = extractFolderIdFromUrl(photoUrl);
                            List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);
                            List<String> imageUrls = new ArrayList<>();
                            for (com.google.api.services.drive.model.File file : imageFiles) {
                                imageUrls.add(file.getWebViewLink());
                            }
                            carListings.add(new CarListingResponse(carData, imageUrls));
                        } else {
                            log.warn("No photo URL found in car data");
                        }
                    }

                    if (carListings.isEmpty()) {
                        log.warn("No car data found in response");
                        throw new RuntimeException("No car data found in response");
                    }

                    log.info("Successfully fetched {} car listings", carListings.size());
                    return carListings;
                } else {
                    log.error("Failed to fetch data. Status code: {}", response.statusCode());
                    throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                LoggerFactory.getLogger(this.getClass()).error("HTTP request failed", e);
                throw new RuntimeException("Failed to send HTTP request", e);
            }
        });
    }

    private String prepareFiltersParameter(FilterGroup filterGroup) throws Exception {
        FilterGroup newFilterGroup = new FilterGroup();
        newFilterGroup.setGroupOperator(filterGroup.getGroupOperator());

        List<Filter> newFilters = new ArrayList<>();
        for (Filter filter : filterGroup.getGroup()) {
            String apiName = filter.getField().getApi_name();
            String id = ApiNameToIdMapper.getIdForApiName(apiName);
            Field newField = new Field(apiName, id);
            Filter newFilter = new Filter(filter.getComparator(), newField, filter.getValue());
            newFilters.add(newFilter);
        }

        newFilterGroup.setGroup(newFilters);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(newFilterGroup);
        return URLEncoder.encode(json, StandardCharsets.UTF_8);
    }

}

