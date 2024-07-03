package com.mudah.auto.cars.config;

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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DriveQuickstart {

    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String SERVICE_ACCOUNT_EMAIL = "mudahauto@imagedrive-344109.iam.gserviceaccount.com";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);
    private static final String FOLDER_LINK = "1zRq-Hv1raIJ_Azys4pT4g2krZ8dH5-PR";

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws GeneralSecurityException, IOException {
        return new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
                .setServiceAccountPrivateKeyFromP12File(new java.io.File("C:\\Users\\bhuva\\Downloads\\mudah-auto-api-service (1)\\mudah-auto-api-service\\src\\main\\resources\\imagedrive-344109-9aaa7a2296a4.p12")) // Use the input stream loaded from resources
                .setServiceAccountScopes(SCOPES)
                .build();
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        try {
            List<File> imageFiles = retrieveImageFiles(service, FOLDER_LINK);
            if (imageFiles.isEmpty()) {
                System.out.println("No image files found in the folder.");
            } else {
                System.out.println("Image Files:");
                for (File file : imageFiles) {
                    System.out.printf("%s: %s\n", file.getName(), file.getWebViewLink());
                }
            }
        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static List<File> retrieveImageFiles(Drive service, String folderId) throws IOException {
        List<File> imageFiles = new ArrayList<>();
        String pageToken = null;
        do {
            FileList result = service.files().list()
                    .setQ("'" + folderId + "' in parents and (mimeType='image/jpeg' or mimeType='image/png')")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, webViewLink)")
                    .setPageToken(pageToken)
                    .execute();
            imageFiles.addAll(result.getFiles());
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return imageFiles;
    }
}
