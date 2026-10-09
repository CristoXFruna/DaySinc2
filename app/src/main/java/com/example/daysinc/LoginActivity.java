package com.example.daysinc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.daysinc.models.User;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etPassword;
    private TextView tvLoginTitle, tvToggleMode;
    private Button btnLogin;
    private boolean isRegisterMode = false;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = AppDatabase.getInstance(this);

        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        btnLogin = findViewById(R.id.btnLogin);

        tvToggleMode.setOnClickListener(v -> setRegisterMode(!isRegisterMode));

        btnLogin.setOnClickListener(v -> {
            String user = etUser.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isRegisterMode) {
                // REGISTRAR EN LA BASE DE DATOS LOCAL (ROOM)
                Executors.newSingleThreadExecutor().execute(() -> {
                    User existingUser = db.userDao().getUserByName(user);
                    if (existingUser == null) {
                        User newUser = new User(user, pass, 0);
                        db.userDao().registerUser(newUser);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "¡Usuario creado exitosamente!", Toast.LENGTH_SHORT).show();
                            navigateToMain(user);
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "El nombre de usuario ya existe", Toast.LENGTH_SHORT).show());
                    }
                });

            } else {
                // INICIAR SESIÓN CON ROOM
                Executors.newSingleThreadExecutor().execute(() -> {
                    User validUser = db.userDao().login(user, pass);
                    runOnUiThread(() -> {
                        if (validUser != null) {
                            navigateToMain(validUser.getUsername());
                        } else {
                            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });
    }

    private void setRegisterMode(boolean register) {
        isRegisterMode = register;
        if (register) {
            tvLoginTitle.setText("Crear Cuenta");
            btnLogin.setText("Registrarse e Ingresar");
            tvToggleMode.setText("¿Ya tienes cuenta? Inicia sesión aquí");
        } else {
            tvLoginTitle.setText("Iniciar Sesión");
            btnLogin.setText("Ingresar");
            tvToggleMode.setText("¿No tienes cuenta? Regístrate aquí");
        }
    }

    private void navigateToMain(String username) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }
}