package com.example.daysinc;

import android.provider.BaseColumns;

/** Contrato con el esquema de tablas y columnas para la base de datos SQLite. */
public final class TaskContract {

    /** Constructor privado para evitar instanciación. */
    private TaskContract() {}

    /** Definición de la tabla y columnas de tareas. */
    public static class TaskEntry implements BaseColumns {
        public static final String TABLE_NAME = "tasks";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_TIME = "scheduled_time";
        public static final String COLUMN_IS_COMPLETED = "is_completed";
        public static final String COLUMN_USER_ID = "user_id";
    }

    /** Definición de la tabla y columnas de usuarios. */
    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_USERNAME = "username";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_STREAK = "streak";
    }
}