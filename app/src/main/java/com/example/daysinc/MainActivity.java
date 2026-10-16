package com.example.daysinc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daysinc.adapters.TaskAdapter;
import com.example.daysinc.models.Task;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private EditText etNewTask;
    private TextView tvStreak;
    private TextView tvNextTask;
    private MaterialButton btnSelectTime;

    private Calendar selectedTimeCalendar;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int PERMISSION_REQUEST_CODE = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        requestPermissions();

        tvStreak = findViewById(R.id.tvStreak);
        tvNextTask = findViewById(R.id.tvNextTask);
        etNewTask = findViewById(R.id.etNewTask);
        rvTasks = findViewById(R.id.rvTasks);
        btnSelectTime = findViewById(R.id.btnSelectTime);

        MaterialButton btnAddTask = findViewById(R.id.btnAddTask);
        MaterialButton btnOpenMap = findViewById(R.id.btnOpenMap);
        MaterialButton btnSendEmail = findViewById(R.id.btnSendEmail);
        MaterialButton btnTestNotification = findViewById(R.id.btnTestNotification);

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, (task, position, targetState) -> {
            verifyAndChangeTaskState(task, position, targetState);
        });

        if (rvTasks != null) {
            rvTasks.setLayoutManager(new LinearLayoutManager(this));
            rvTasks.setAdapter(taskAdapter);
        }

        btnSelectTime.setOnClickListener(v -> showTimePickerDialog());

        if (btnAddTask != null) {
            btnAddTask.setOnClickListener(v -> addTaskWithReminder());
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        updateStreakLogic();

        if (btnOpenMap != null) {
            btnOpenMap.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
            });
        }

        if (btnSendEmail != null) {
            btnSendEmail.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("message/rfc822");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"estudiante@ejemplo.com"});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Recordatorio DaySinc");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "¡Recuerda completar tus tareas de estudio hoy!");
                try {
                    startActivity(Intent.createChooser(emailIntent, "Enviar correo mediante..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, "No hay clientes de correo instalados.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnTestNotification != null) {
            btnTestNotification.setOnClickListener(v -> triggerImmediateTestNotification());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NotificationReceiver.CHANNEL_ID,
                    "Recordatorios de Actividades",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canal oficial de notificaciones DaySinc");
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    private void verifyAndChangeTaskState(Task task, int position, boolean targetState) {
        if (!targetState) {
            task.setCompleted(false);
            taskAdapter.notifyItemChanged(position);
            return;
        }

        SharedPreferences prefs = getSharedPreferences("StudyZonePrefs", MODE_PRIVATE);
        boolean isSet = prefs.getBoolean("zone_set", false);

        if (!isSet) {
            Toast.makeText(this, "⚠️ Primero debes configurar tu Zona de Actividades en el mapa", Toast.LENGTH_LONG).show();
            return;
        }

        float savedLat = prefs.getFloat("zone_lat", 0);
        float savedLng = prefs.getFloat("zone_lng", 0);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Concede permisos de ubicación para verificar la tarea", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                float[] results = new float[1];
                Location.distanceBetween(
                        location.getLatitude(), location.getLongitude(),
                        savedLat, savedLng, results
                );

                float distanceInMeters = results[0];
                if (distanceInMeters <= 100) {
                    new AlertDialog.Builder(this)
                            .setTitle("✅ Verificación de Actividad")
                            .setMessage("¿Confirmas que realizaste la actividad '" + task.getTitle() + "' en tu Zona de Actividades?")
                            .setPositiveButton("Sí, confirmar", (dialog, which) -> {
                                task.setCompleted(true);
                                taskAdapter.notifyItemChanged(position);
                                Toast.makeText(this, "🎉 ¡Excelente trabajo! Actividad completada.", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("🔴 Verificación Fallida")
                            .setMessage("Debes estar físicamente dentro de tu Zona de Actividades para marcar esta tarea como realizada.\n\nDistancia actual: " + (int) distanceInMeters + " metros.")
                            .setPositiveButton("Entendido", null)
                            .show();
                }
            } else {
                Toast.makeText(this, "Obteniendo posición GPS...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTimePickerDialog() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    selectedTimeCalendar = Calendar.getInstance();
                    selectedTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTimeCalendar.set(Calendar.MINUTE, minute);
                    selectedTimeCalendar.set(Calendar.SECOND, 0);
                    selectedTimeCalendar.set(Calendar.MILLISECOND, 0);

                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    btnSelectTime.setText("⏰ Hora: " + formattedTime);
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void addTaskWithReminder() {
        String taskTitle = etNewTask.getText().toString().trim();

        if (taskTitle.isEmpty()) {
            Toast.makeText(this, "Escribe una actividad primero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTimeCalendar == null) {
            Toast.makeText(this, "⚠️ Debes seleccionar una hora obligatoria para la actividad", Toast.LENGTH_LONG).show();
            return;
        }

        String timeString = String.format(Locale.getDefault(), "%02d:%02d",
                selectedTimeCalendar.get(Calendar.HOUR_OF_DAY),
                selectedTimeCalendar.get(Calendar.MINUTE));

        taskList.add(new Task(taskTitle + " (" + timeString + ")", false));
        taskAdapter.notifyItemInserted(taskList.size() - 1);

        scheduleNotification5MinutesBefore(taskTitle, selectedTimeCalendar.getTimeInMillis());

        etNewTask.setText("");
        selectedTimeCalendar = null;
        btnSelectTime.setText("⏰ Seleccionar Hora *");

        Toast.makeText(this, "✅ Actividad guardada con recordatorio de 5 min antes", Toast.LENGTH_SHORT).show();
    }

    private void scheduleNotification5MinutesBefore(String title, long timeInMillis) {
        long notifyTime = timeInMillis - (5 * 60 * 1000);

        if (notifyTime <= System.currentTimeMillis()) {
            notifyTime = System.currentTimeMillis() + 3000;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("title", "⏰ Próxima Actividad (En 5 min)");
        intent.putExtra("message", "Recuerda hacer tus actividades: " + title);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(notifyTime, pendingIntent);
            try {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
            } catch (Exception e) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTime, pendingIntent);
            }
        }
    }

    private void triggerImmediateTestNotification() {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("title", "🔔 Recordatorio DaySinc");
        intent.putExtra("message", "Recuerda hacer tus actividades");
        sendBroadcast(intent);
        Toast.makeText(this, "Enviando notificación...", Toast.LENGTH_SHORT).show();
    }

    private void updateStreakLogic() {
        SharedPreferences prefs = getSharedPreferences("DaySincPrefs", MODE_PRIVATE);
        int currentStreak = prefs.getInt("user_streak", 0);
        String lastDate = prefs.getString("last_streak_date", "");
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (!todayDate.equals(lastDate)) {
            currentStreak++;
            prefs.edit()
                    .putInt("user_streak", currentStreak)
                    .putString("last_streak_date", todayDate)
                    .apply();
        }

        if (tvStreak != null) {
            tvStreak.setText("🔥 Racha Actual: " + currentStreak + " días seguidos");
        }
    }

    private void checkStudyZoneStatus() {
        SharedPreferences prefs = getSharedPreferences("StudyZonePrefs", MODE_PRIVATE);
        boolean isSet = prefs.getBoolean("zone_set", false);

        if (!isSet) {
            if (tvNextTask != null) {
                tvNextTask.setText("📍 Zona de Actividades: No configurada");
            }
            return;
        }

        float savedLat = prefs.getFloat("zone_lat", 0);
        float savedLng = prefs.getFloat("zone_lng", 0);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                float[] results = new float[1];
                Location.distanceBetween(
                        location.getLatitude(), location.getLongitude(),
                        savedLat, savedLng, results
                );

                float distanceInMeters = results[0];
                if (distanceInMeters <= 100) {
                    tvNextTask.setText("🟢 Estás DENTRO de tu Zona de Actividades. ¡Habilitado para trabajar!");
                } else {
                    tvNextTask.setText("🔴 Estás FUERA de la Zona de Actividades (" + (int) distanceInMeters + "m de distancia)");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkStudyZoneStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkStudyZoneStatus();
        }
    }
}