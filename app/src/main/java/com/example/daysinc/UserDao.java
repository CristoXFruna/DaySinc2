package com.example.daysinc;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.daysinc.models.User;

@Dao
public interface UserDao {

    @Insert
    void registerUser(User user);

    @Query("SELECT * FROM users_table WHERE username = :username AND password = :password LIMIT 1")
    User login(String username, String password);

    @Query("SELECT * FROM users_table WHERE username = :username LIMIT 1")
    User getUserByName(String username);
}