package com.mudah.auto.cars.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mudah.auto.cars.service.CarService;
import com.mudah.auto.cars.service.dto.Field;
import com.mudah.auto.cars.service.dto.Filter;
import com.mudah.auto.cars.service.dto.FilterGroup;
import com.mudah.auto.cars.util.ApiNameToIdMapper;
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

@Service
public class CarServiceImpl implements CarService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient;

    public CarServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<Object> getCarLists(String accessToken) {
        String url = "https://crm.zoho.com/crm/v2.2/Inventories/bulk?";

        return Mono.fromCallable(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Zoho-oauthtoken " + accessToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), Object.class);
        });
    }

    @Override
    public Mono<Object> getCarDetails(String id,String accessToken) {
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
                return objectMapper.readValue(response.body(), Object.class);
            } else {
                throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
            }
        });
    }

    @Override
    public Mono<Object> getCarListsFilter(String accessToken, String comparator, String apiName, Object value) {
        String url = "https://crm.zoho.com/crm/v2.1/Inventories/bulk"; // Ensure correct version

        String fieldId;
        try {
            fieldId = ApiNameToIdMapper.getIdForApiName(apiName);
        } catch (IllegalArgumentException e) {
            return Mono.error(e);
        }
        Filter filter = new Filter(comparator, new Field(apiName, fieldId),value);
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
                    return objectMapper.readValue(response.body(), Object.class);
                } else {
                    LoggerFactory.getLogger(this.getClass()).error("Failed to fetch data. Status code: " + response.statusCode() + ", body: " + response.body());
                    throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode() + ", body: " + response.body());
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
                    return objectMapper.readValue(response.body(), Object.class);
                } else {
                    LoggerFactory.getLogger(this.getClass()).error("Failed to fetch data. Status code: " + response.statusCode() + ", body: " + response.body());
                    throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode() + ", body: " + response.body());
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

//    @Override
//    public Mono<CarDetailsResponse> getCarDetails(String id, String accessToken) {
//        String url = "https://crm.zoho.com/crm/v2.2/Inventories/" + id +
//                "?approved=both&converted=both&formatted_currency=true&home_converted_currency=true&on_demand_properties=%24client_portal_permission";
//
//        return Mono.fromCallable(() -> {
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .header("Authorization", "Zoho-oauthtoken " + accessToken)
//                    .GET()
//                    .build();
//
//            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
//
//            if (response.statusCode() == 200) {
//                JsonNode rootNode = objectMapper.readTree(response.body());
//                JsonNode dataNode = rootNode.path("data");
//                if (dataNode.isArray() && dataNode.size() > 0) {
//                    JsonNode firstItem = dataNode.get(0);
//                    String photoUrl = firstItem.path("Photo_URL").asText();
//                    List<String> photoUrls = Collections.emptyList();
//                    if (photoUrl != null && photoUrl.contains("drive.google.com/drive/folders/")) {
//                        String folderId = photoUrl.substring(photoUrl.lastIndexOf('/') + 1, photoUrl.indexOf('?'));
//                        List<File> files = GoogleDriveService.listFilesInFolder(folderId); // List files in the folder
//                        photoUrls = files.stream()
//                                .filter(file -> file.getMimeType().startsWith("image/"))
//                                .map(file -> "https://drive.google.com/uc?id=" + file.getId())
//                                .collect(Collectors.toList());
//                    }
//                    Map<String, Object> carDetailsMap = objectMapper.convertValue(firstItem, Map.class);
//                    return new CarDetailsResponse(carDetailsMap, photoUrls);
//                }
//                return new CarDetailsResponse(Collections.emptyMap(), Collections.emptyList());
//            } else {
//                throw new RuntimeException("Failed to fetch data. Status code: " + response.statusCode());
//            }
//        });
//    }

}

