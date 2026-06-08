package com.myfarmio.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Task {
    @SerializedName("id")
    public String id;

    @SerializedName("organization_id")
    public String organizationId;

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("module_key")
    public String moduleKey;

    @SerializedName("category")
    public String category;

    // "pending" | "planned" | "in_progress" | "completed" | "cancelled"
    @SerializedName("status")
    public String status;

    // "low" | "medium" | "high" | "urgent"
    @SerializedName("priority")
    public String priority;

    @SerializedName("responsible_user_id")
    public String responsibleUserId;

    @SerializedName("start_date")
    public String startDate;

    @SerializedName("due_date")
    public String dueDate;

    @SerializedName("zone")
    public String zone;

    @SerializedName("progress")
    public int progress;

    @SerializedName("observations")
    public String observations;

    // Posible lista de archivos/attachments desde Supabase (opcional)
    @SerializedName("attachments")
    public List<String> attachments;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    @SerializedName("completed_at")
    public String completedAt;

    // Helpers para UI
    public boolean isCompleted() { return "completed".equals(status); }
    public boolean isPending() { return "pending".equals(status) || "planned".equals(status); }
    public boolean isInProgress() { return "in_progress".equals(status); }
    public boolean isCritical() { return "urgent".equals(priority); }
}
