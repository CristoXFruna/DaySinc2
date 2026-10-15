package com.example.daysinc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.daysinc.models.Task;
import com.example.daysinc.models.User;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "daysinc_native.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TaskContract.UserEntry.TABLE_NAME + " ("
                + TaskContract.UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TaskContract.UserEntry.COLUMN_USERNAME + " TEXT UNIQUE, "
                + TaskContract.UserEntry.COLUMN_EMAIL + " TEXT, "
                + TaskContract.UserEntry.COLUMN_PASSWORD + " TEXT, "
                + TaskContract.UserEntry.COLUMN_STREAK + " INTEGER DEFAULT 0)";

        String CREATE_TASKS_TABLE = "CREATE TABLE " + TaskContract.TaskEntry.TABLE_NAME + " ("
                + TaskContract.TaskEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TaskContract.TaskEntry.COLUMN_TITLE + " TEXT, "
                + TaskContract.TaskEntry.COLUMN_CATEGORY + " TEXT, "
                + TaskContract.TaskEntry.COLUMN_TIME + " INTEGER, "
                + TaskContract.TaskEntry.COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0, "
                + TaskContract.TaskEntry.COLUMN_USER_ID + " TEXT)";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_TASKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TaskContract.UserEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TaskContract.TaskEntry.TABLE_NAME);
        onCreate(db);
    }

    public boolean registerUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TaskContract.UserEntry.COLUMN_USERNAME, username);
        values.put(TaskContract.UserEntry.COLUMN_EMAIL, email);
        values.put(TaskContract.UserEntry.COLUMN_PASSWORD, password);
        values.put(TaskContract.UserEntry.COLUMN_STREAK, 0);

        long result = db.insert(TaskContract.UserEntry.TABLE_NAME, null, values);
        return result != -1;
    }

    public boolean loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = TaskContract.UserEntry.COLUMN_USERNAME + " = ? AND " + TaskContract.UserEntry.COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {username, password};

        Cursor cursor = db.query(
                TaskContract.UserEntry.TABLE_NAME,
                null, selection, selectionArgs, null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean insertTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TaskContract.TaskEntry.COLUMN_TITLE, task.getTitle());
        values.put(TaskContract.TaskEntry.COLUMN_CATEGORY, task.getCategory());
        values.put(TaskContract.TaskEntry.COLUMN_TIME, task.getScheduledTime());
        values.put(TaskContract.TaskEntry.COLUMN_IS_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(TaskContract.TaskEntry.COLUMN_USER_ID, task.getUserId());

        long result = db.insert(TaskContract.TaskEntry.TABLE_NAME, null, values);
        return result != -1;
    }

    public List<Task> getTasksByUser(String userId) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = TaskContract.TaskEntry.COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {userId};

        Cursor cursor = db.query(
                TaskContract.TaskEntry.TABLE_NAME,
                null, selection, selectionArgs, null, null, TaskContract.TaskEntry.COLUMN_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry._ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_TITLE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_CATEGORY));
                long time = cursor.getLong(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_TIME));
                boolean isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_IS_COMPLETED)) == 1;
                String user = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_USER_ID));

                Task task = new Task(title, category, time, isCompleted, user);
                task.setId(id);
                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return taskList;
    }

    public boolean updateTaskState(int taskId, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TaskContract.TaskEntry.COLUMN_IS_COMPLETED, isCompleted ? 1 : 0);

        String selection = TaskContract.TaskEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(taskId)};

        int count = db.update(TaskContract.TaskEntry.TABLE_NAME, values, selection, selectionArgs);
        return count > 0;
    }

    public boolean deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = TaskContract.TaskEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(taskId)};

        int deletedRows = db.delete(TaskContract.TaskEntry.TABLE_NAME, selection, selectionArgs);
        return deletedRows > 0;
    }
}