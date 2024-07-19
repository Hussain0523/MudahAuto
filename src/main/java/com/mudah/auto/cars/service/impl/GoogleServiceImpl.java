package com.mudah.auto.cars.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GoogleServiceImpl {

    @Value("${google.p12.file.path}")
    private String googleP12fileUrl;

    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String SERVICE_ACCOUNT_EMAIL = "mudahauto@imagedrive-344109.iam.gserviceaccount.com";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws GeneralSecurityException, IOException {
        log.info("Creating Google credentials.");
        return new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
                .setServiceAccountPrivateKeyFromP12File(new java.io.File(googleP12fileUrl))
                .setServiceAccountScopes(SCOPES)
                .build();
    }

    private Drive getDriveService() throws GeneralSecurityException, IOException {
        log.info("Building Drive service.");
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<File> retrieveImageFiles(String folderId) throws GeneralSecurityException, IOException {
        log.info("Retrieving image files from folder: {}", folderId);
        Drive service = getDriveService();
        List<File> imageFiles = getImagesInFolder(service, folderId);
        log.info("Retrieved {} image files from folder: {}", imageFiles.size(), folderId);
        return imageFiles;
    }

    private List<File> getImagesInFolder(Drive service, String folderId) throws IOException {
        log.info("Fetching images in folder: {}", folderId);
        List<File> imageFiles = new ArrayList<>();
        String pageToken = null;
        do {
            FileList result = service.files().list()
                    .setQ("'" + folderId + "' in parents and (mimeType='image/jpeg' or mimeType='image/png')")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, webViewLink, mimeType)")
                    .setPageToken(pageToken)
                    .execute();
            for (File file : result.getFiles()) {
                log.debug("File found: {} ({}) - mimeType: {}", file.getName(), file.getId(), file.getMimeType());
                if (!file.getName().startsWith("RC")) {
                    imageFiles.add(file);
                } else {
                    log.debug("Skipping file: {} ({})", file.getName(), file.getId());
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        log.info("Total images fetched from folder {}: {}", folderId, imageFiles.size());
        return imageFiles;
    }
}
