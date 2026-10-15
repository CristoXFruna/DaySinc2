package com.example.daysinc.models;

public class Task {

    private int id;
    private String title;
    private String category;
    private long scheduledTime;
    private boolean isCompleted;
    private String userId;

    public Task(String title, String category, long scheduledTime, boolean isCompleted, String userId) {
        this.title = title;
        this.category = category;
        this.scheduledTime = scheduledTime;
        this.isCompleted = isCompleted;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}