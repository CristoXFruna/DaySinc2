package com.example.daysinc;
import com.example.daysinc.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daysinc.models.AlarmReceiver;
import com.example.daysinc.models.Task;
import com.example.daysinc.models.TaskAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity {

    private TextView tvStreak, tvNextTask;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStreak = findViewById(R.id.tvStreak);
        tvNextTask = findViewById(R.id.tvNextTask);
        recyclerView = findViewById(R.id.recyclerViewTasks);
        FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask);



        db = AppDatabase.getInstance(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter((task, isChecked) -> {
            task.setCompleted(isChecked);
            Executors.newSingleThreadExecutor().execute(() -> {
                db.taskDao().update(task);
                updateNextTask();
            });
        });
        recyclerView.setAdapter(adapter);

        fabAddTask.setOnClickListener(v -> createQuickTask());

        loadTasks();
    }

    private void loadTasks() {
        db.taskDao().getAllTasks().observe(this, tasks -> {
            adapter.setTasks(tasks);
            updateNextTask();
        });
    }

    private void createQuickTask() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 1);

        Task newTask = new Task("Estudiar Programación", "Universidad", cal.getTimeInMillis(), false);

        Executors.newSingleThreadExecutor().execute(() -> {
            db.taskDao().insert(newTask);
            scheduleAlarm(newTask.getTitle(), cal.getTimeInMillis());
        });

        Toast.makeText(this, "Tarea agregada. Alarma lista en 1 min", Toast.LENGTH_SHORT).show();
    }

    private void scheduleAlarm(String taskTitle, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("task_title", taskTitle);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) triggerAtMillis,
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
            Task nextTask = db.taskDao().getNextPendingTask();
            runOnUiThread(() -> {
                if (nextTask != null) {
                    tvNextTask.setText("Siguiente: " + nextTask.getTitle() + " (" + nextTask.getCategory() + ")");
                } else {
                    tvNextTask.setText("Siguiente: Ninguna tarea pendiente");
                }
            });
        });
    }
}