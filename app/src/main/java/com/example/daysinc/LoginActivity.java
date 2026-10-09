package com.example.daysinc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etPassword;
    private TextView tvLoginTitle, tvToggleMode;
    private Button btnLogin;
    private boolean isRegisterMode = false;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferences = getSharedPreferences("DaySincPrefs", Context.MODE_PRIVATE);

        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        btnLogin = findViewById(R.id.btnLogin);

        if (!preferences.contains("registered_user")) {
            setRegisterMode(true);
        }

        tvToggleMode.setOnClickListener(v -> setRegisterMode(!isRegisterMode));

        btnLogin.setOnClickListener(v -> {
            String user = etUser.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isRegisterMode) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("registered_user", user);
                editor.putString("registered_pass", pass);
                editor.apply();

                Toast.makeText(this, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show();
                navigateToMain(user);
            } else {
                String savedUser = preferences.getString("registered_user", "");
                String savedPass = preferences.getString("registered_pass", "");

                if (user.equals(savedUser) && pass.equals(savedPass)) {
                    navigateToMain(user);
                } else {
                    Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                }
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