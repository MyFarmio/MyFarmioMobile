package com.myfarmio.app.models;

import com.google.gson.annotations.SerializedName;

public class Plot {
    @SerializedName("id")
    public String id;

    @SerializedName("organization_id")
    public String organizationId;

    @SerializedName("farm_id")
    public String farmId;

    @SerializedName("name")
    public String name;

    // "agriculture" | "livestock" | "mixed" | "forestry" | "other"
    @SerializedName("plot_type")
    public String plotType;

    @SerializedName("area_hectares")
    public double areaHectares;

    @SerializedName("current_crop")
    public String currentCrop;

    // "optimal" | "monitoring" | "harvest" | "resting" | "issue" | "inactive"
    @SerializedName("status")
    public String status;

    @SerializedName("crop_stage")
    public String cropStage;

    @SerializedName("soil_moisture_percent")
    public Double soilMoisturePercent;

    @SerializedName("estimated_yield")
    public String estimatedYield;

    @SerializedName("responsible_user_id")
    public String responsibleUserId;

    @SerializedName("zone")
    public String zone;

    @SerializedName("rotation")
    public String rotation;

    @SerializedName("next_action")
    public String nextAction;

    @SerializedName("notes")
    public String notes;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    public boolean hasIssue() { return "issue".equals(status); }
    public boolean isOptimal() { return "optimal".equals(status); }
}
