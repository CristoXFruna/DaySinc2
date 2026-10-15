package com.example.daysinc;

import android.provider.BaseColumns;

public final class TaskContract {

    private TaskContract() {}
    public static class TaskEntry implements BaseColumns {
        public static final String TABLE_NAME = "tasks";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_TIME = "scheduled_time";
        public static final String COLUMN_IS_COMPLETED = "is_completed";
        public static final String COLUMN_USER_ID = "user_id";
    }
    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_USERNAME = "username";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_STREAK = "streak";
    }
}