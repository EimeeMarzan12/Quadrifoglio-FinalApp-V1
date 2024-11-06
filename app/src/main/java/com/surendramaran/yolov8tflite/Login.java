package com.surendramaran.yolov8tflite;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class Login extends AppCompatActivity {
    private EditText usernameField;
    private EditText passwordField;
    private ImageView hiddenButton;
    private Button login;
    private DatabaseReference mUserRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        hiddenButton = findViewById(R.id.ic_queen);
        usernameField = findViewById(R.id.username_editText);
        passwordField = findViewById(R.id.password_editText);
        login = findViewById(R.id.login_button);

        // Reference to "users" node in Firebase Realtime Database
        mUserRef = FirebaseDatabase.getInstance().getReference("users");

        hiddenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent e = new Intent(Login.this, AddUser.class);
                startActivity(e);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                String password = passwordField.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Login.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                } else {
                    loginUser(username, password);
                }
            }
        });
    }

    private void loginUser(String username, String password) {
        mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean userFound = false;
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    HashMap<String, Object> userMap = (HashMap<String, Object>) userSnapshot.getValue();

                    if (userMap != null && userMap.get("username").equals(username)) {
                        userFound = true;
                        String storedPassword = (String) userMap.get("password");

                        if (storedPassword != null && storedPassword.equals(password)) {
                            // Successful login
                            Intent intent = new Intent(Login.this, MainActivity.class);
                            intent.putExtra("username_key", username);
                            intent.putExtra("password_key", password);
                            startActivity(intent);
                            finish();
                            return;
                        } else {
                            passwordField.setText("");
                            Toast.makeText(Login.this, "Incorrect Password, try again", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                if (!userFound) {
                    Toast.makeText(Login.this, "Username not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("TAG", "Failed to read value:", error.toException());
                Toast.makeText(Login.this, "Error accessing database", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
