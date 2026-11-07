package com.example.skillswap;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saved_skills")
public class SavedSkillEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String category;
    private String description;
    private String authorName;
    private String location;
    private String status;
    private String imageUrl;

    public SavedSkillEntity(@NonNull String id, String title, String category, String description, String authorName, String location, String status, String imageUrl) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.authorName = authorName;
        this.location = location;
        this.status = status;
        this.imageUrl = imageUrl != null ? imageUrl : "";
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getAuthorName() { return authorName; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }

    public SkillPost toSkillPost() {
        return new SkillPost(id, title, category, description, authorName, location, status, imageUrl);
    }
}
