package com.example.daysinc;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daysinc.models.AlarmReceiver;
import com.example.daysinc.models.StreakWidgetProvider;
import com.example.daysinc.models.Task;
import com.example.daysinc.models.TaskAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView tvStreak, tvNextTask;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private DatabaseHelper dbHelper;
    private String currentUserId = "invitado";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            lastAcceleration = currentAcceleration;
            currentAcceleration = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = currentAcceleration - lastAcceleration;
            acceleration = acceleration * 0.9f + delta;

            if (acceleration > 12) {
                Toast.makeText(MainActivity.this, "¡Teléfono agitado! Recargando tareas...", Toast.LENGTH_SHORT).show();
                loadTasks();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent().hasExtra("username")) {
            currentUserId = getIntent().getStringExtra("username");
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        acceleration = 10f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        tvStreak = findViewById(R.id.tvStreak);
        tvNextTask = findViewById(R.id.tvNextTask);
        recyclerView = findViewById(R.id.recyclerViewTasks);
        FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask);
        MaterialButton btnTestNotification = findViewById(R.id.btnTestNotification);

        dbHelper = new DatabaseHelper(this);

        SharedPreferences prefs = getSharedPreferences("DaySincPrefs", MODE_PRIVATE);
        int userStreak = prefs.getInt("user_streak_" + currentUserId, 0);
        tvStreak.setText("🔥 Racha: " + userStreak + " días (" + currentUserId + ")");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TaskAdapter((task, isChecked) -> {
            if (isChecked) {
                new AlertDialog.Builder(this)
                        .setTitle("¿Completaste esta tarea?")
                        .setMessage("¿Confirmas que terminaste: '" + task.getTitle() + "'?")
                        .setPositiveButton("Sí, la completé", (dialog, which) -> {
                            task.setCompleted(true);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                dbHelper.updateTaskState(task.getId(), true);
                                runOnUiThread(this::updateNextTask);
                            });

                            int updatedStreak = prefs.getInt("user_streak_" + currentUserId, 0) + 1;
                            updateStreak(updatedStreak);

                            Toast.makeText(this, "¡Felicidades " + currentUserId + "! Racha aumentada a " + updatedStreak + " días 🔥", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Aún no", (dialog, which) -> {
                            task.setCompleted(false);
                            adapter.notifyDataSetChanged();
                        })
                        .setOnCancelListener(dialog -> {
                            task.setCompleted(false);
                            adapter.notifyDataSetChanged();
                        })
                        .show();
            } else {
                task.setCompleted(false);
                Executors.newSingleThreadExecutor().execute(() -> {
                    dbHelper.updateTaskState(task.getId(), false);
                    runOnUiThread(this::updateNextTask);
                });
            }
        });

        recyclerView.setAdapter(adapter);

        fabAddTask.setOnClickListener(v -> showCreateTaskDialog());

        btnTestNotification.setOnClickListener(v -> {
            long triggerAt = System.currentTimeMillis() + 5000;
            scheduleAlarm("🔔 Notificación de Prueba DaySinc", triggerAt);
            Toast.makeText(this, "Prueba programada en 5 segundos...", Toast.LENGTH_SHORT).show();
        });

        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorListener);
        }
    }

    private void showCreateTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nueva Tarea para " + currentUserId);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("Nombre del quehacer (Ej: Estudiar)");
        builder.setView(etTitle);

        Calendar selectedCalendar = Calendar.getInstance();

        builder.setPositiveButton("Elegir Hora", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Escribe un título", Toast.LENGTH_SHORT).show();
                return;
            }

            int hour = selectedCalendar.get(Calendar.HOUR_OF_DAY);
            int minute = selectedCalendar.get(Calendar.MINUTE);

            TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                cal.set(Calendar.MINUTE, minuteOfHour);
                cal.set(Calendar.SECOND, 0);

                if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                Task newTask = new Task(title, "Personal", cal.getTimeInMillis(), false, currentUserId);

                Executors.newSingleThreadExecutor().execute(() -> {
                    dbHelper.insertTask(newTask);
                    scheduleAlarm(newTask.getTitle(), cal.getTimeInMillis());
                    runOnUiThread(this::loadTasks);
                });

                String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                Toast.makeText(this, "Tarea '" + title + "' agendada a las " + timeStr, Toast.LENGTH_LONG).show();

            }, hour, minute, true);

            timePicker.show();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadTasks() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Task> tasks = dbHelper.getTasksByUser(currentUserId);
            runOnUiThread(() -> {
                adapter.setTasks(tasks);
                updateNextTask();
            });
        });
    }

    private void scheduleAlarm(String taskTitle, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("task_title", taskTitle);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }
    }

    private void updateNextTask() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Task> tasks = dbHelper.getTasksByUser(currentUserId);
            Task nextTask = null;
            for (Task t : tasks) {
                if (!t.isCompleted()) {
                    nextTask = t;
                    break;
                }
            }
            Task finalNextTask = nextTask;
            runOnUiThread(() -> {
                if (finalNextTask != null) {
                    tvNextTask.setText("Siguiente: " + finalNextTask.getTitle() + " (" + finalNextTask.getCategory() + ")");
                } else {
                    tvNextTask.setText("Siguiente: Ninguna tarea pendiente");
                }
            });
        });
    }

    private void updateStreak(int newStreak) {
        SharedPreferences prefs = getSharedPreferences("DaySincPrefs", MODE_PRIVATE);
        prefs.edit().putInt("user_streak_" + currentUserId, newStreak).apply();

        tvStreak.setText("🔥 Racha: " + newStreak + " días (" + currentUserId + ")");

        prefs.edit().putInt("user_streak", newStreak).apply();

        Intent intent = new Intent(this, StreakWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), StreakWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }
    private void sendEmailNotification(String userEmail, String taskTitle) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(android.net.Uri.parse("mailto:" + userEmail));
        intent.putExtra(Intent.EXTRA_SUBJECT, "🔔 Recordatorio de Actividad - DaySinc");
        intent.putExtra(Intent.EXTRA_TEXT, "Hola, te recordamos que tienes pendiente tu actividad: '" + taskTitle + "'. ¡No rompas tu racha!");

        try {
            startActivity(Intent.createChooser(intent, "Enviar correo con:"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No hay cliente de correo instalado", Toast.LENGTH_SHORT).show();
        }
    }
}