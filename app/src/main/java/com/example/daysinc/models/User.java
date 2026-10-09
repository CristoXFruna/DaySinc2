package com.example.daysinc.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users_table")
public class User {

    @PrimaryKey
    @NonNull
    private String username;
    private String password;
    private int streak;

    public User(@NonNull String username, String password, int streak) {
        this.username = username;
        this.password = password;
        this.streak = streak;
    }

    @NonNull
    public String getUsername() { return username; }
    public void setUsername(@NonNull String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
}