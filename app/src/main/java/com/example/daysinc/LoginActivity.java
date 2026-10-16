package com.example.daysinc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etEmail, etPassword;
    private TextView tvLoginTitle, tvToggleMode;
    private Button btnLogin;
    private boolean isRegisterMode = false;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        etUser = findViewById(R.id.etUser);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        btnLogin = findViewById(R.id.btnLogin);

        if (etEmail != null) {
            etEmail.setVisibility(View.GONE);
        }

        tvToggleMode.setOnClickListener(v -> setRegisterMode(!isRegisterMode));

        btnLogin.setOnClickListener(v -> {
            String user = etUser.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String email = (etEmail != null) ? etEmail.getText().toString().trim() : "";

            if (user.isEmpty() || pass.isEmpty() || (isRegisterMode && email.isEmpty())) {
                Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isRegisterMode) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    boolean success = dbHelper.registerUser(user, email, pass);
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(this, "¡Usuario registrado exitosamente!", Toast.LENGTH_SHORT).show();
                            navigateToMain(user, email);
                        } else {
                            Toast.makeText(this, "El nombre de usuario ya existe", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } else {
                Executors.newSingleThreadExecutor().execute(() -> {
                    boolean isValid = dbHelper.loginUser(user, pass);
                    runOnUiThread(() -> {
                        if (isValid) {
                            navigateToMain(user, email);
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
        if (etEmail != null) {
            etEmail.setVisibility(register ? View.VISIBLE : View.GONE);
        }
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

    private void navigateToMain(String username, String email) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }
}