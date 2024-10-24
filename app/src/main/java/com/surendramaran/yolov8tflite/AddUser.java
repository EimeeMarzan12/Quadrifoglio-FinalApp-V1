package com.surendramaran.yolov8tflite;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;

public class AddUser extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private int userCount = 1;
    private EditText usernameField;
    private EditText passwordField;
    private Button addButton;
    private DatabaseReference mUserRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        User user = new User();
        mUserRef = DatabaseManager.getUserRef();

        addButton = (Button) findViewById(R.id.add_button);
        usernameField = (EditText) findViewById(R.id.username_add);
        passwordField = (EditText) findViewById(R.id.password_add);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = usernameField.getText().toString();
                user.setUserUname(userName);
                user.setUserPass(passwordField.getText().toString());
                user.setUserId(userCount);

                mUserRef.child(userName).setValue(user);

                Toast.makeText(AddUser.this, "User added!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(AddUser.this, Login.class);
                startActivity(intent);
            }
        });
    }
}