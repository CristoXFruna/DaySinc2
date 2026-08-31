package com.example.daysinc;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
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

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks_table ORDER BY timestamp ASC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks_table WHERE isCompleted = 0 ORDER BY timestamp ASC LIMIT 1")
    Task getNextPendingTask();
}