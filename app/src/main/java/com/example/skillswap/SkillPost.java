package com.example.skillswap;

public class SkillPost {
    private String postId;
    private String title;
    private String category;
    private String description;
    private String authorName;
    private String location;
    private String status;

    public SkillPost() {}

    public SkillPost(String postId, String title, String category, String description, String authorName, String location, String status) {
        this.postId = postId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.authorName = authorName;
        this.location = location;
        this.status = status;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}