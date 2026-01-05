package com.example.classmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.username_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);

        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        TextView createAccountButton = findViewById(R.id.create_account_button);
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleLogin() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "נא למלא שם משתמש וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isAdmin = false;
        if (username.equals("admin") && password.equals("123456")) {
            isAdmin = true;
            Toast.makeText(LoginActivity.this, "התחברת כמנהל מערכת", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(LoginActivity.this, "ברוך הבא: " + username, Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("USERNAME", username);
        intent.putExtra("IS_ADMIN", isAdmin);
        startActivity(intent);
        finish();
    }
}
