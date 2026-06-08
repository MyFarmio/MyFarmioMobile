package com.myfarmio.app.models;

import com.google.gson.annotations.SerializedName;

public class Farm {
    @SerializedName("id")
    public String id;

    @SerializedName("organization_id")
    public String organizationId;

    @SerializedName("name")
    public String name;

    @SerializedName("location")
    public String location;

    @SerializedName("area_hectares")
    public Double areaHectares;

    @SerializedName("owner_id")
    public String ownerId;

    @SerializedName("notes")
    public String notes;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    public boolean hasArea() { return areaHectares != null && areaHectares > 0; }
}
