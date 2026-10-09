package com.example.daysinc;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.daysinc.models.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    void insert(Task task);

    @Update
    void update(Task task);

    @Query("SELECT * FROM tasks_table WHERE userId = :userId ORDER BY timestamp ASC")
    LiveData<List<Task>> getTasksByUser(String userId);

    @Query("SELECT * FROM tasks_table WHERE userId = :userId AND isCompleted = 0 ORDER BY timestamp ASC LIMIT 1")
    Task getNextPendingTaskByUser(String userId);
}