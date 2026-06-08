package com.myfarmio.app.models;

import com.google.gson.annotations.SerializedName;

public class AppUser {
    @SerializedName("id")
    public String id;

    @SerializedName("full_name")
    public String fullName;

    @SerializedName("email")
    public String email;

    @SerializedName("avatar_url")
    public String avatarUrl;

    @SerializedName("role_title")
    public String roleTitle;

    @SerializedName("phone")
    public String phone;

    public String getDisplayName() {
        if (fullName != null && !fullName.isEmpty()) return fullName;
        if (email != null && !email.isEmpty()) return email.split("@")[0];
        return "Usuario";
    }

    public String getInitials() {
        if (fullName == null || fullName.isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                 + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(fullName.charAt(0)).toUpperCase();
    }
}
