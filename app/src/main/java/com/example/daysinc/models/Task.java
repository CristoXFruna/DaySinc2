package com.example.daysinc.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks_table")
public class Task {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String category;
    private long timestamp;
    private boolean isCompleted;

    public Task(String title, String category, long timestamp, boolean isCompleted) {
        this.title = title;
        this.category = category;
        this.timestamp = timestamp;
        this.isCompleted = isCompleted;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public long getTimestamp() { return timestamp; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}