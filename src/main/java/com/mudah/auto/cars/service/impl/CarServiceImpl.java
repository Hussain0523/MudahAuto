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
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("5741151000001402995", "5741151000001402995");
                hashMap.put("5741151000006366228", "5741151000006366228");
                hashMap.put("5741151000008823014", "5741151000008823014");
                hashMap.put("5741151000009020281", "5741151000009020281");
                hashMap.put("5741151000009320063", "5741151000009320063");
                hashMap.put("5741151000015258386", "5741151000015258386");
                hashMap.put("5741151000019764378", "5741151000019764378");
                hashMap.put("5741151000021696078", "5741151000021696078");
                hashMap.put("5741151000022850752", "5741151000022850752");

                List<CarListingResponse> matchingCarListings = new ArrayList<>();
                List<CarListingResponse> nonMatchingCarListings = new ArrayList<>();

                for (Map<String, Object> carData : data) {
                    String id = (String) carData.get("id");
                    if (hashMap.containsKey(id)) {
                        String photoUrl = (String) carData.get("Photo_URL");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            String folderId = extractFolderIdFromUrl(photoUrl);

                            List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);
                            List<String> imageUrls = new ArrayList<>();
                            List<String> otherImageUrls = new ArrayList<>();

                            for (com.google.api.services.drive.model.File file : imageFiles) {
                                String webViewLink = file.getWebViewLink();
                                if (isPredefinedUrl(webViewLink)) {
                                    imageUrls.add(webViewLink);
                                } else {
                                    otherImageUrls.add(webViewLink);
                                }
                            }
                            imageUrls.addAll(imageUrls.size(), otherImageUrls);

                            matchingCarListings.add(new CarListingResponse(carData, imageUrls));
                        } else {
                            matchingCarListings.add(new CarListingResponse(carData, new ArrayList<>()));
                        }
                    } else {
                        nonMatchingCarListings.add(new CarListingResponse(carData, new ArrayList<>()));
                    }
                }

                List<CarListingResponse> carListingResponses = new ArrayList<>(matchingCarListings);
                carListingResponses.addAll(nonMatchingCarListings);

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
                    List<String> otherImageUrls = new ArrayList<>();

                    for (com.google.api.services.drive.model.File file : imageFiles) {
                        String webViewLink = file.getWebViewLink();
                        if (isPredefinedUrl(webViewLink)) {
                            imageUrls.add(webViewLink);
                        } else {
                            otherImageUrls.add(webViewLink);
                        }
                    }
                    imageUrls.addAll(imageUrls.size(), otherImageUrls);

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
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("5741151000001402995","5741151000001402995");
                    hashMap.put("5741151000006366228","5741151000006366228");
                    hashMap.put("5741151000008823014","5741151000008823014");
                    hashMap.put("5741151000009020281","5741151000009020281");
                    hashMap.put("5741151000009320063","5741151000009320063");
                    hashMap.put("5741151000015258386","5741151000015258386");
                    hashMap.put("5741151000019764378","5741151000019764378");
                    hashMap.put("5741151000021696078","5741151000021696078");
                    hashMap.put("5741151000022850752","5741151000022850752");
                    List<CarListingResponse> carListings = new ArrayList<>();
                    for (Map<String, Object> carData : data) {
                        String id= (String) carData.get("id");
                        String photoUrl = (String) carData.get("Photo_URL");
                        if(hashMap.get(id)!=null) {
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                String folderId = extractFolderIdFromUrl(photoUrl);
                                List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);
                                List<String> imageUrls = new ArrayList<>();
                                List<String> otherImageUrls = new ArrayList<>();

                                for (com.google.api.services.drive.model.File file : imageFiles) {
                                    String webViewLink = file.getWebViewLink();
                                    if (isPredefinedUrl(webViewLink)) {
                                        imageUrls.add(webViewLink);
                                    } else {
                                        otherImageUrls.add(webViewLink);
                                    }
                                }
                                imageUrls.addAll(imageUrls.size(), otherImageUrls);
                                carListings.add(new CarListingResponse(carData, imageUrls));
                            } else {
                                log.warn("No photo URL found in car data");
                            }
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
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("5741151000001402995", "5741151000001402995");
                    hashMap.put("5741151000006366228", "5741151000006366228");
                    hashMap.put("5741151000008823014", "5741151000008823014");
                    hashMap.put("5741151000009020281", "5741151000009020281");
                    hashMap.put("5741151000009320063", "5741151000009320063");
                    hashMap.put("5741151000015258386", "5741151000015258386");
                    hashMap.put("5741151000019764378", "5741151000019764378");
                    hashMap.put("5741151000021696078", "5741151000021696078");
                    hashMap.put("5741151000022850752", "5741151000022850752");

                    List<CarListingResponse> matchingCarListings = new ArrayList<>();
                    List<CarListingResponse> nonMatchingCarListings = new ArrayList<>();

                    for (Map<String, Object> carData : data) {
                        String id = (String) carData.get("id");
                        String photoUrl = (String) carData.get("Photo_URL");

                        if (hashMap.containsKey(id)) {
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                String folderId = extractFolderIdFromUrl(photoUrl);
                                List<com.google.api.services.drive.model.File> imageFiles = googleService.retrieveImageFiles(folderId);
                                List<String> imageUrls = new ArrayList<>();
                                List<String> otherImageUrls = new ArrayList<>();

                                for (com.google.api.services.drive.model.File file : imageFiles) {
                                    String webViewLink = file.getWebViewLink();
                                    if (isPredefinedUrl(webViewLink)) {
                                        imageUrls.add(webViewLink);
                                    } else {
                                        otherImageUrls.add(webViewLink);
                                    }
                                }
                                imageUrls.addAll(imageUrls.size(), otherImageUrls);
                                matchingCarListings.add(new CarListingResponse(carData, imageUrls));
                            } else {
                                log.warn("No photo URL found in car data");
                            }
                        } else {
                            nonMatchingCarListings.add(new CarListingResponse(carData, new ArrayList<>()));
                        }
                    }

                    if (matchingCarListings.isEmpty() && nonMatchingCarListings.isEmpty()) {
                        log.warn("No car data found in response");
                        throw new RuntimeException("No car data found in response");
                    }

                    List<CarListingResponse> carListings = new ArrayList<>(matchingCarListings);
                    carListings.addAll(nonMatchingCarListings);

                    log.info("Successfully fetched {} car listings", carListings.size());
                    return carListings;
                } else {
                    log.error("Failed to fetch data. Status code: {}", response.statusCode());
                    throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                String errorMessage = "Failed to send HTTP request";
                LoggerFactory.getLogger(this.getClass()).error(errorMessage, e);
                // You can rethrow the caught exception with a more descriptive RuntimeException
                throw new RuntimeException(errorMessage, e);
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

    private boolean isPredefinedUrl(String url) {
        Set<String> predefinedUrls = new HashSet<>(Arrays.asList(
                "https://drive.google.com/file/d/1BVCemUfgPlz3kLF42vqLiEuIohA_ETbW/view?usp=drivesdk",
                "https://drive.google.com/file/d/134NvO5G1hXwMoRgWRNuY2TQh9yeAUSKw/view?usp=drivesdk",
                "https://drive.google.com/file/d/1XMRhhYXIL0rtgzWlp3UWchkdY8SeoYhW/view?usp=drivesdk",
                "https://drive.google.com/file/d/1CVtSw5dPOQ-Ot7a-j8ykGDTkAJCSv6Bf/view?usp=drivesdk",
                "https://drive.google.com/file/d/1JaqSV-S11zGq2SdJH3I_v8HWIyNXwi9g/view?usp=drivesdk",
                "https://drive.google.com/file/d/1glXEY2Y5MXoKf6uHYrKS0O3rGdCsWchO/view?usp=drivesdk",
                "https://drive.google.com/file/d/159vbFADpUSlGl9qTPbel6fzH9kkBOVkx/view?usp=drivesdk",
                "https://drive.google.com/file/d/1CxnTCw0DWIeH3S7UYduxs0UjguBlqC9G/view?usp=drivesdk",
                "https://drive.google.com/file/d/12kI0raDiV4x2-ipJpdsgr33sgMHgsqFD/view?usp=drivesdk",
                "https://drive.google.com/file/d/1zHAm5lKwIhhkfh0I1XuDMy0LvEyH5oNs/view?usp=drivesdk",
                "https://drive.google.com/file/d/1-wZEpwdCfhFwDxy0dyYqJvlsguPI0pKE/view?usp=drivesdk",
                "https://drive.google.com/file/d/1UFIXtR55QiRxurtsrUfvzOhONfIIObOs/view?usp=drivesdk",
                "https://drive.google.com/file/d/1doz4E8zpPoAQ4czidOxs3OtocSgvBg_V/view?usp=drivesdk",
                "https://drive.google.com/file/d/1noN4mTHg0wTO0TRlH0CQwdjMpG43MQ0a/view?usp=drivesdk",
                "https://drive.google.com/file/d/1ZTguObBHC4dfO2MBEbKdjFS8utmP13n5/view?usp=drivesdk",
                "https://drive.google.com/file/d/1Em2S52JiThb92sosLp8Z5h54T0TFpUDd/view?usp=drivesdk",
                "https://drive.google.com/file/d/1u5rCJoxF7FGvVL_45G8_I_4ti1Jt2bUb/view?usp=drivesdk",
                "https://drive.google.com/file/d/16GA5wP3EIcX2GMOSI7N4A49CQB9QtPvx/view?usp=drivesdk",
                "https://drive.google.com/file/d/1t6EGB1LCmwzGD8_niFpbbiqJc5fRjqm7/view?usp=drivesdk",
                "https://drive.google.com/file/d/1-AS78WVqYCOoPfZWOkg13SjiuZDBxqLx/view?usp=drivesdk",
                "https://drive.google.com/file/d/1NvJV7X0RPztP8RukENG40rUdn15-hDmV/view?usp=drivesdk",
                "https://drive.google.com/file/d/10l3jdGjJtJS3_lVmaI8NJ92zXBeFPSJ9/view?usp=drivesdk",
                "https://drive.google.com/file/d/102yCxwxQnozLO76nZEGM488QR5VpIGwb/view?usp=drivesdk",
                "https://drive.google.com/file/d/1jB5D39tVSnBNIiFJcDWtQaKzob9zHDBe/view?usp=drivesdk",
                "https://drive.google.com/file/d/1kEWiHYAjnRMSDCw9zClC_348JyalzvyP/view?usp=drivesdk",
                "https://drive.google.com/file/d/17rM8zQn9WhMi7cI4Gao73tkaX6l3XfeD/view?usp=drivesdk",
                "https://drive.google.com/file/d/1hICgBJK6_Ct1nFh4Ysmh_ZiGNQjo7NU8/view?usp=drivesdk",
                "https://drive.google.com/file/d/1Papj9tdg1DV86WXixhj403BnoAGnhiJf/view?usp=drivesdk",
                "https://drive.google.com/file/d/1bp-y4tcyIBNCcMQUnziVTpoosgQFHzxi/view?usp=drivesdk",
                "https://drive.google.com/file/d/1_F-b6T_ZQU3ehRKKTWVSiFf69AsQLaWO/view?usp=drivesdk",
                "https://drive.google.com/file/d/1b3qsxI9l9FlZ2xTGw_cpw9fSODIzo6up/view?usp=drivesdk",
                "https://drive.google.com/file/d/1DLPP5KRkZSscTAfrsK6PB8lK4pgqVbBg/view?usp=drivesdk",
                "https://drive.google.com/file/d/18biQ0orskYT_rflpvFR205012RkLk3sX/view?usp=drivesdk",
                "https://drive.google.com/file/d/1uT5EAAVNIyr8irl-uaSpLcaQkEk4HBf9/view?usp=drivesdk",
                "https://drive.google.com/file/d/1lPEY3seVbTYLJ7cb1HSDK6_93CvxVR_r/view?usp=drivesdk",
                "https://drive.google.com/file/d/10qiIwy-86lVg2Eg4PqBMwcINr5lXFiEi/view?usp=drivesdk",
                "https://drive.google.com/file/d/1_vK-v9qLWtFMbgj3-8zhNATF2V3VkAh7/view?usp=drivesdk"
        ));

        return predefinedUrls.contains(url);
    }
}

