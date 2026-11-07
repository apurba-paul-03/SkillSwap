package com.example.skillswap;

public class SkillPost {
    private String id;
    private String title;
    private String category;
    private String description;
    private String authorName;
    private String location;
    private String status;
    private String imageUrl;
    private String phoneNumber;

    // Empty constructor required for Firebase Firestore toObject() mapping
    public SkillPost() {}

    public SkillPost(String id, String title, String category, String description, String authorName, String location, String status) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.authorName = authorName;
        this.location = location;
        this.status = status;
        this.imageUrl = "";
        this.phoneNumber = "01700000000";
    }

    public SkillPost(String id, String title, String category, String description, String authorName, String location, String status, String imageUrl) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.authorName = authorName;
        this.location = location;
        this.status = status;
        this.imageUrl = imageUrl;
        this.phoneNumber = "01700000000";
    }

    public SkillPost(String id, String title, String category, String description, String authorName, String location, String status, String imageUrl, String phoneNumber) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.authorName = authorName;
        this.location = location;
        this.status = status;
        this.imageUrl = imageUrl;
        this.phoneNumber = phoneNumber != null && !phoneNumber.trim().isEmpty() ? phoneNumber : "01700000000";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getAuthorName() { return authorName; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPhoneNumber() {
        return phoneNumber != null && !phoneNumber.trim().isEmpty() ? phoneNumber : "01700000000";
    }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
