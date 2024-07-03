package com.mudah.auto.cars.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CarListingResponse {

    @Setter
    private CarListing data;

    @Getter
    @Setter
    public static class CarListing {

        private Owner owner;

        @JsonProperty("Extended_Warranty_Provider")
        private String extendedWarrantyProvider;

        @JsonProperty("$currency_symbol")
        private String currencySymbol;

        @JsonProperty("TCEC_Handling_Fee")
        private int TCECHandlingFee;

        @JsonProperty("$field_states")
        private String fieldStates;

        @JsonProperty("$review_process")
        private ReviewProcess reviewProcess;

        @JsonProperty("Photo_URL")
        private String photoURL;

        @JsonProperty("Mileage_km")
        private int mileageKm;

        @JsonProperty("$followers")
        private String followers;

        @JsonProperty("$sharing_permission")
        private String sharingPermission;

        @JsonProperty("Year_of_EWP")
        private String yearOfEWP;

        @JsonProperty("Car_Brand_Car_Make")
        private String carBrandCarMake;

        @JsonProperty("$canvas_id")
        private String canvasId;

        @JsonProperty("Full_Variant")
        private String fullVariant;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("Colour")
        private String colour;

        @JsonProperty("Last_Activity_Time")
        private LocalDateTime lastActivityTime;

        @JsonProperty("Extended_Warranty_Program_Yes_No")
        private String extendedWarrantyProgramYesNo;

        @JsonProperty("$review")
        private String review;

        @JsonProperty("$state")
        private String state;

        @JsonProperty("Unsubscribed_Mode")
        private String unsubscribedMode;

        @JsonProperty("$process_flow")
        private boolean processFlow;

        @JsonProperty("$locked_for_me")
        private boolean lockedForMe;

        @JsonProperty("id")
        private String id;

        @JsonProperty("$zia_visions")
        private String ziaVisions;

        @JsonProperty("$approved")
        private boolean approved;

        @JsonProperty("Availability")
        private String availability;

        @JsonProperty("$approval")
        private Approval approval;

        @JsonProperty("Refurbishment_Details")
        private List<RefurbishmentDetail> refurbishmentDetails;

        @JsonProperty("Unsubscribed_Time")
        private LocalDateTime unsubscribedTime;

        @JsonProperty("$followed")
        private boolean followed;

        @JsonProperty("$wizard_connection_path")
        private String wizardConnectionPath;

        @JsonProperty("$editable")
        private boolean editable;

        @JsonProperty("$orchestration")
        private boolean orchestration;

        @JsonProperty("Car_Model")
        private String carModel;

        @JsonProperty("Variant")
        private String variant;

        @JsonProperty("Year_Make")
        private String yearMake;

        @JsonProperty("$in_merge")
        private boolean inMerge;

        @JsonProperty("Locked__s")
        private boolean lockedS;

        @JsonProperty("Current_Location")
        private String currentLocation;

        @JsonProperty("Total_Refurbishment_Cost")
        private double totalRefurbishmentCost;

        @JsonProperty("Listing_Price")
        private double listingPrice;

        @JsonProperty("Tag")
        private List<String> tag;

        @JsonProperty("$zia_owner_assignment")
        private String ziaOwnerAssignment;

        @JsonProperty("$approval_state")
        private String approvalState;

        @JsonProperty("$pathfinder")
        private boolean pathfinder;

        @JsonProperty("Purchaser_Hub")
        private String purchaserHub;

        private List<String> base64Images = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Owner {
        @JsonProperty("name")
        private String name;

        @JsonProperty("id")
        private String id;

        @JsonProperty("email")
        private String email;
    }

    @Getter
    @Setter
    public static class ReviewProcess {

        @JsonProperty("approve")
        private boolean approve;

        @JsonProperty("reject")
        private boolean reject;

        @JsonProperty("resubmit")
        private boolean resubmit;
    }

    @Getter
    @Setter
    public static class Approval {

        @JsonProperty("delegate")
        private boolean delegate;

        @JsonProperty("approve")
        private boolean approve;

        @JsonProperty("reject")
        private boolean reject;

        @JsonProperty("resubmit")
        private boolean resubmit;
    }

    @Getter
    @Setter
    public static class RefurbishmentDetail {
        @JsonProperty("$in_merge")
        private boolean inMerge;

        @JsonProperty("$field_states")
        private String fieldStates;

        @JsonProperty("Created_Time")
        private LocalDateTime createdTime;

        @JsonProperty("Parent_Id")
        private ParentId parentId;

        private String id;

        @JsonProperty("$zia_visions")
        private String ziaVisions;
    }

    @Getter
    @Setter
    public static class ParentId {

        @JsonProperty("name")
        private String name;

        @JsonProperty("id")
        private String id;
    }

}
