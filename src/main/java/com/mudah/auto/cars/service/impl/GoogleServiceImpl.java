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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleServiceImpl {

    @Value("${google.p12.file.path}")
    private String googleP12fileUrl;

    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String SERVICE_ACCOUNT_EMAIL = "mudahauto@imagedrive-344109.iam.gserviceaccount.com";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws GeneralSecurityException, IOException {
        return new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
                .setServiceAccountPrivateKeyFromP12File(new java.io.File(googleP12fileUrl))
                .setServiceAccountScopes(SCOPES)
                .build();
    }

    private Drive getDriveService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<File> retrieveImageFiles(String folderId) throws GeneralSecurityException, IOException {
        Drive service = getDriveService();
        List<File> imageFiles = new ArrayList<>();
        String pageToken = null;
        do {
            FileList result = service.files().list()
                    .setQ("'" + folderId + "' in parents and mimeType='application/vnd.google-apps.folder'")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();
            for (File file : result.getFiles()) {
                if (file.getName().toLowerCase().startsWith("edited")) {
                    imageFiles.addAll(getImagesInFolder(service, file.getId()));
                    break;
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return imageFiles;
    }

    private List<File> getImagesInFolder(Drive service, String folderId) throws IOException {
        List<File> imageFiles = new ArrayList<>();
        String pageToken = null;
        do {
            FileList result = service.files().list()
                    .setQ("'" + folderId + "' in parents and (mimeType='image/jpeg' or mimeType='image/png')")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, webViewLink)")
                    .setPageToken(pageToken)
                    .execute();
            for (File file : result.getFiles()) {
                if (!file.getName().startsWith("RC")) {
                    imageFiles.add(file);
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return imageFiles;
    }
}
