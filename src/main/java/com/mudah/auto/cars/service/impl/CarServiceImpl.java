package com.mudah.auto.cars.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mudah.auto.cars.payload.CarCountResponse;
import com.mudah.auto.cars.payload.CarListingResponse;
import com.mudah.auto.cars.service.CarService;
import com.mudah.auto.cars.service.dto.Field;
import com.mudah.auto.cars.service.dto.Filter;
import com.mudah.auto.cars.service.dto.FilterGroup;
import com.mudah.auto.cars.util.ApiNameToIdMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
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
import java.util.stream.Collectors;

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
                log.info("Received successful response from Zoho CRM.");
                Map<String, Object> carDetails = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) carDetails.get("data");

                List<CarListingResponse> carListingResponses = new ArrayList<>();

                for (Map<String, Object> carData : data) {
                    String photoUrl = (String) carData.get("Photo_URL_for_Reseller");
                    List<String> imageUrls = new ArrayList<>();

                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        try {
                            String folderId = extractFolderIdFromUrl(photoUrl);
                            List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);

                            for (com.google.api.services.drive.model.File file : imageFiles) {
                                String webViewLink = file.getWebViewLink();
                                imageUrls.add(webViewLink);
                            }

                            log.debug("Added {} image URLs for folder ID: {}", imageUrls.size(), folderId);

                        } catch (Exception e) {
                            log.error("Error retrieving images for URL: {}", photoUrl, e);
                        }
                    } else {
                        log.warn("Empty or null photo URL for car ID: {}", carData.get("id"));
                    }

                    carListingResponses.add(new CarListingResponse(carData, imageUrls));
                }

                log.info("Total car listings processed: {}", carListingResponses.size());
                return carListingResponses;
            } else {
                log.error("Failed to fetch data. Status code: {}", response.statusCode());
                throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
            }
        });
    }

    @Override
    public Mono<CarListingResponse> getCarDetails(String id, String accessToken) {
        String url = "https://crm.zoho.com/crm/v2.2/Inventories/" + id +
                "?approved=both&converted=both&formatted_currency=true&home_converted_currency=true&on_demand_properties=%24client_portal_permission";

        return Mono.fromCallable(() -> {
            log.info("Creating HTTP request to fetch car details for id: {}", id);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Zoho-oauthtoken " + accessToken)
                    .GET()
                    .build();

            log.info("Sending HTTP request to URL: {}", url);
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Received successful response for id: {}", id);
                Map<String, Object> carDetails = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                List<Map<String, Object>> data = (List<Map<String, Object>>) carDetails.get("data");

                if (!data.isEmpty()) {
                    Map<String, Object> carData = data.getFirst();
                    String photoUrl = (String) carData.get("Photo_URL_for_Reseller");

                    List<String> imageUrls = new ArrayList<>();
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        try {
                            String folderId = extractFolderIdFromUrl(photoUrl);
                            log.info("Extracted folder ID: {}", folderId);

                            List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);

                            if (imageFiles != null && !imageFiles.isEmpty()) {
                                // Sort files by name
                                imageFiles.sort(Comparator.comparing(com.google.api.services.drive.model.File::getName));

                                for (com.google.api.services.drive.model.File file : imageFiles) {
                                    String webViewLink = file.getWebViewLink();
                                    imageUrls.add(webViewLink);
                                }

                                log.debug("Added {} image URLs for folder ID: {}", imageUrls.size(), folderId);
                            } else {
                                log.warn("No image files found for folder ID: {}", folderId);
                            }
                        } catch (Exception e) {
                            log.error("Error retrieving images for photo URL: {}", photoUrl, e);
                        }
                    } else {
                        log.warn("Empty or null photo URL for car ID: {}", id);
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
        }).doOnError(e -> log.error("Error occurred while fetching car details for id: {}", id, e));
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
    public Mono<Object> getCarListsMultiFilter(String accessToken, FilterGroup filterGroup) throws Exception {
        String url = "https://crm.zoho.com/crm/v2.1/Inventories/bulk";
        String cvidParam = "cvid=" + URLEncoder.encode("5741151000000625050", StandardCharsets.UTF_8);
        String filtersParam = prepareFiltersParameter(filterGroup);
        String formData = cvidParam + "&filters=" + filtersParam;

        return Mono.fromCallable(() -> {
            try {
                log.info("Creating HTTP request to fetch filtered car lists with access token.");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Zoho-oauthtoken " + accessToken)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formData))
                        .build();

                log.info("Sending HTTP request to URL: {}", url);
                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    log.info("Received successful response for filtered car lists.");
                    Map<String, Object> jsonResponse = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
                    List<Map<String, Object>> data = (List<Map<String, Object>>) jsonResponse.get("data");

                    List<CarListingResponse> carListings = new ArrayList<>();
                    for (Map<String, Object> carData : data) {
                        String photoUrl = (String) carData.get("Photo_URL_for_Reseller");

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            try {
                                String folderId = extractFolderIdFromUrl(photoUrl);

                                List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);

                                // Sort files by name
                                imageFiles.sort(Comparator.comparing(com.google.api.services.drive.model.File::getName));

                                List<String> imageUrls = new ArrayList<>();

                                for (com.google.api.services.drive.model.File file : imageFiles) {
                                    String webViewLink = file.getWebViewLink();
                                    imageUrls.add(webViewLink);
                                }

                                log.debug("Added {} image URLs for folder ID: {}", imageUrls.size(), folderId);
                                carListings.add(new CarListingResponse(carData, imageUrls));
                            } catch (Exception e) {
                                log.error("Error retrieving images for photo URL: {}", photoUrl, e);
                            }
                        } else {
                            log.warn("No photo URL found in car data: {}", carData);
                            carListings.add(new CarListingResponse(carData, new ArrayList<>()));
                        }
                    }

                    if (carListings.isEmpty()) {
                        log.warn("No car data found in response.");
                        throw new RuntimeException("No car data found in response.");
                    }

                    log.info("Successfully fetched {} car listings.", carListings.size());
                    return carListings;
                } else {
                    log.error("Failed to fetch data. Status code: {}", response.statusCode());
                    throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                log.error("Failed to send HTTP request", e);
                throw new RuntimeException("Failed to send HTTP request", e);
            }
        });
    }


    @Override
    public Mono<List<CarListingResponse>> getCarListsWithoutImages(String accessToken, Integer page, Integer perPage) {
        String url = "https://crm.zoho.com/crm/v2.2/Inventories/bulk?page=" + page + "&per_page=" + perPage;

        return Mono.fromCallable(() -> {
            log.info("Creating HTTP request to fetch car listings.");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Zoho-oauthtoken " + accessToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            log.info("Sending HTTP request to URL: {}", url);
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Received successful response from Zoho CRM.");
                Map<String, Object> carDetails = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> data = (List<Map<String, Object>>) carDetails.get("data");

                List<CarListingResponse> carListingResponses = new ArrayList<>();
                for (Map<String, Object> carData : data) {
                    CarListingResponse carListingResponse = new CarListingResponse(carData, new ArrayList<>());
                    carListingResponses.add(carListingResponse);
                    log.debug("Added car listing with ID: {}", carData.get("id"));
                }

                log.info("Total car listings fetched: {}", carListingResponses.size());
                return carListingResponses;
            } else {
                log.error("Failed to fetch data. Status code: {}", response.statusCode());
                throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
            }
        });
    }

    @Override
    public Mono<List<String>> getCarImages(List<String> photoUrls) {
        return Flux.fromIterable(photoUrls)
                .flatMap(photoUrl -> Mono.fromCallable(() -> {
                    List<String> imageUrls = new ArrayList<>();

                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        try {
                            log.info("Processing photo URL: {}", photoUrl);

                            String folderId = extractFolderIdFromUrl(photoUrl);
                            log.info("Extracted folder ID: {}", folderId);

                            List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);

                            // Sort files by name
                            imageFiles.sort(Comparator.comparing(com.google.api.services.drive.model.File::getName));

                            for (com.google.api.services.drive.model.File file : imageFiles) {
                                String webViewLink = file.getWebViewLink();
                                imageUrls.add(webViewLink);
                                log.debug("Added image URL: {}", webViewLink);
                            }

                            log.info("Total images retrieved from folder {}: {}", folderId, imageFiles.size());

                        } catch (Exception e) {
                            log.error("Error processing photo URL: {}", photoUrl, e);
                        }
                    } else {
                        log.warn("Empty or null photo URL: {}", photoUrl);
                    }

                    return imageUrls;
                }))
                .collectList()
                .flatMap(imageUrlsList -> {
                    List<String> allImageUrls = imageUrlsList.stream().flatMap(List::stream).collect(Collectors.toList());
                    log.info("Total images retrieved: {}", allImageUrls.size());
                    return Mono.just(allImageUrls);
                });
    }

    @Override
    public Mono<CarCountResponse> getTotalCount(String accessToken) {
        String url = "https://crm.zoho.com/crm/v2.2/Inventories/actions/count?approved=both&cvid=5741151000000625050&home_converted_currency=true&formatted_currency=true";

        return Mono.fromCallable(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Zoho-oauthtoken " + accessToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("Received successful response from Zoho CRM.");
                String responseBody = response.body();
                log.info("Response body: {}", responseBody); // Log response body for debugging
                CarCountResponse carCountResponse = objectMapper.readValue(responseBody, CarCountResponse.class);
                return carCountResponse;
            } else {
                log.error("Failed to fetch data. Status code: {}", response.statusCode());
                throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
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

